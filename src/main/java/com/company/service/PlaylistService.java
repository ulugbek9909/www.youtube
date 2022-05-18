package com.company.service;

import com.company.dto.ChannelDTO;
import com.company.dto.PlaylistAboutDTO;
import com.company.dto.PlaylistDTO;
import com.company.dto.VideoDTO;
import com.company.entity.ChannelEntity;
import com.company.entity.PlaylistEntity;
import com.company.entity.ProfileEntity;
import com.company.enums.PlaylistStatus;
import com.company.enums.ProfileRole;
import com.company.exception.AppBadRequestException;
import com.company.exception.AppForbiddenException;
import com.company.exception.ItemNotFoundException;
import com.company.repository.PlaylistRepository;
import com.company.repository.PlaylistVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final ChannelService channelService;
    private final ProfileService profileService;
    private final PlaylistVideoRepository playlistVideoRepository;

    private final VideoService videoService;


    public PlaylistDTO create(PlaylistDTO dto, Integer channelId, Integer profileId) {
        ChannelEntity channelEntity = channelService.getById(channelId);

        if (!channelEntity.getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        PlaylistEntity entity = new PlaylistEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStatus(PlaylistStatus.PUBLIC);
        entity.setChannelId(channelEntity.getId());
        entity.setOrderNum(dto.getOrderNum());

        try {
            playlistRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.warn("Unique {}", dto);
            throw new AppBadRequestException("Unique!");
        }
        return toFullDTO(entity);
    }

    public PlaylistDTO updateAbout(PlaylistAboutDTO dto, Integer playlistId, Integer profileId) {
        PlaylistEntity entity = getById(playlistId);

        if (!entity.getChannel().getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setOrderNum(dto.getOrderNum());
        entity.setUpdatedDate(LocalDateTime.now());
        try {
            playlistRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.warn("Unique {}", dto);
            throw new AppBadRequestException("Unique!");
        }
        return toFullDTO(entity);
    }


    public PageImpl<PlaylistDTO> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        List<PlaylistDTO> dtoList = new ArrayList<>();

        Page<PlaylistEntity> entityPage = playlistRepository.findAll(pageable);

        entityPage.forEach(entity -> dtoList.add(toFullDTO(entity)));
        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }

    public Boolean changeStatus(Integer playlistId, Integer profileId) {
        PlaylistEntity entity = getById(playlistId);

        ProfileEntity profileEntity = profileService.getById(profileId);

        if (entity.getChannel().getProfileId().equals(profileId) ||
                profileEntity.getRole().equals(ProfileRole.ADMIN)) {

            switch (entity.getStatus()) {
                case PUBLIC -> playlistRepository.updateStatus(PlaylistStatus.PRIVATE, entity.getId());
                case PRIVATE -> playlistRepository.updateStatus(PlaylistStatus.PUBLIC, entity.getId());
            }

            return true;
        }
        log.warn("Not access {}", profileId);
        throw new AppForbiddenException("Not access!");
    }

    public List<PlaylistDTO> channelPlaylist(Integer channelId) {
        ChannelEntity channelEntity = channelService.getById(channelId);

        List<PlaylistDTO> dtoList = new ArrayList<>();

        List<PlaylistEntity> entityList = playlistRepository.findAllByChannelIdAndStatus(channelEntity.getId(),
                PlaylistStatus.PUBLIC,
                Sort.by(Sort.Direction.DESC, "orderNum"));

        entityList.forEach(entity -> dtoList.add(toShortDTO(entity)));
        return dtoList;
    }

    public List<PlaylistDTO> profilePlaylist(Integer profileId) {
        ProfileEntity profileEntity = profileService.getById(profileId);

        List<PlaylistDTO> dtoList = new ArrayList<>();

        List<PlaylistEntity> entityList = playlistRepository.findAllByProfileId(profileEntity.getId(),
                Sort.by(Sort.Direction.DESC, "orderNum"));

        entityList.forEach(entity -> dtoList.add(toShortDTO(entity)));
        return dtoList;
    }

    public Boolean delete(Integer playlistId, Integer profileId) {
        PlaylistEntity entity = getById(playlistId);

        if (!entity.getChannel().getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        playlistRepository.delete(entity);

        return true;
    }

    public PlaylistDTO get(Integer playlistId) {
        PlaylistEntity entity = getById(playlistId);
        return toFullDTO(entity);
    }

    public PlaylistEntity getById(Integer id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    throw new ItemNotFoundException("Not found!");
                });
    }

    public PlaylistDTO toShortDTO(PlaylistEntity entity) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());

        ChannelDTO ChannelDTO = new ChannelDTO(channelService.toOpenUrl(entity.getChannelId().toString()));
        dto.setChannel(ChannelDTO);

        dto.setVideoCount(playlistVideoRepository.getVideoCountByPlaylistId(entity.getId()));


        List<VideoDTO> videoList = playlistVideoRepository.getTop2VideoByPlaylistId(entity.getId())
                .stream()
                .map(playlistVideoEntity -> {
                    VideoDTO videoDTO = new VideoDTO();
                    videoDTO.setId(playlistVideoEntity.getVideoId());
                    videoDTO.setTitle(playlistVideoEntity.getVideo().getTitle());
                    videoDTO.setUrl(videoService.toOpenUrl(playlistVideoEntity.getVideoId().toString()));
                    videoDTO.setDuration(playlistVideoEntity.getVideo().getDuration());
                    return videoDTO;
                }).toList();

        dto.setVideoList(videoList);
        dto.setCreatedDate(entity.getCreatedDate());
        return dto;
    }

    public PlaylistDTO toFullDTO(PlaylistEntity entity) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());

        ChannelDTO ChannelDTO = new ChannelDTO(channelService.toOpenUrl(entity.getChannelId().toString()));
        dto.setChannel(ChannelDTO);

        dto.setStatus(entity.getStatus());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }
}

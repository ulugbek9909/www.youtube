package com.company.service;

import com.company.dto.*;
import com.company.entity.ChannelEntity;
import com.company.entity.PlaylistEntity;
import com.company.entity.ProfileEntity;
import com.company.enums.PlaylistStatus;
import com.company.enums.ProfileRole;
import com.company.exception.AppBadRequestException;
import com.company.exception.AppForbiddenException;
import com.company.exception.ItemNotFoundException;
import com.company.mapper.PlayListInfoAdminMapper;
import com.company.mapper.PlayListInfoJpqlAdminMapper;
import com.company.repository.PlaylistRepository;
import com.company.repository.PlaylistVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final ChannelService channelService;
    private final ProfileService profileService;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final AttachService attachService;
    private final VideoService videoService;


    public PlaylistDTO create(PlaylistDTO dto, String channelId, String profileId) {
        ChannelEntity channelEntity = channelService.getById(channelId);

        if (!channelEntity.getProfileId().toString().equals(profileId)) {
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

    public PlaylistDTO updateAbout(PlaylistAboutDTO dto, String playlistId, String profileId) {
        PlaylistEntity entity = getById(playlistId);

        if (!entity.getChannel().getProfileId().toString().equals(profileId)) {
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
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Pageable pageable = PageRequest.of(page, size);

        List<PlaylistDTO> dtoList = new ArrayList<>();

//       1 Page<PlaylistEntity> entityPage = playlistRepository.findAll(pageable);
        Page<PlayListInfoAdminMapper> entityPage = playlistRepository.getPlaylistInfoForAdmin(pageable);

        List<PlayListInfoAdminMapper> entityList = entityPage.getContent();
        List<PlaylistDTO> playListDTO = new LinkedList<>();
        entityList.forEach(entity -> {
            PlaylistDTO dto = new PlaylistDTO();
            dto.setId(entity.getPl_id());
            dto.setName(entity.getPl_name());
            dto.setDescription(entity.getPl_description());

            ChannelDTO channelDTO = new ChannelDTO();
            channelDTO.setId(entity.getCh_id());
            channelDTO.setName(entity.getCh_name());
            if (entity.getCh_photo_id() != null) {
                AttachDTO attachDTO = new AttachDTO(attachService.toOpenUrl(entity.getCh_photo_id()));
                channelDTO.setPhoto(attachDTO);
            }

            dto.setChannel(channelDTO);

            ProfileDTO profileDTO = new ProfileDTO();
            profileDTO.setId(entity.getPr_id());
            profileDTO.setName(entity.getPr_name());
            profileDTO.setSurname(entity.getPr_surname());

            if (Optional.ofNullable(entity.getPr_photo_id()).isPresent()) {
                AttachDTO attachDTO = new AttachDTO(attachService.toOpenUrl(entity.getPr_photo_id()));
                profileDTO.setImage(attachDTO);
            }
            channelDTO.setProfile(profileDTO);

            playListDTO.add(dto);
        });
//        entityPage.forEach(entity -> {
//            dtoList.add(toFullDTO(entity));
//        });
//        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
        return new PageImpl<>(playListDTO, pageable, entityPage.getTotalElements());
    }

    public PageImpl<PlaylistDTO> list_jpql(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        List<PlaylistDTO> dtoList = new ArrayList<>();

        Page<PlayListInfoJpqlAdminMapper> entityPage = playlistRepository.getPlaylistInfoJpql(pageable);
        return null;
    }

    public Boolean changeStatus(String playlistId, String profileId) {
        PlaylistEntity entity = getById(playlistId);

        ProfileEntity profileEntity = profileService.getById(profileId);

        if (entity.getChannel().getProfileId().toString().equals(profileId) ||
                profileEntity.getRole().equals(ProfileRole.ADMIN)) {

            switch (entity.getStatus()) {
                case PUBLIC -> {
                    playlistRepository.updateStatus(PlaylistStatus.PRIVATE, entity.getId());
                }
                case PRIVATE -> {
                    playlistRepository.updateStatus(PlaylistStatus.PUBLIC, entity.getId());
                }
            }

            return true;
        }
        log.warn("Not access {}", profileId);
        throw new AppForbiddenException("Not access!");
    }

    public List<PlaylistDTO> channelPlaylist(String channelId) {
        ChannelEntity channelEntity = channelService.getById(channelId);

        List<PlaylistDTO> dtoList = new ArrayList<>();

        List<PlaylistEntity> entityList = playlistRepository.findAllByChannelIdAndStatus(channelEntity.getId(),
                PlaylistStatus.PUBLIC,
                Sort.by(Sort.Direction.DESC, "orderNum"));

        entityList.forEach(entity -> {
            dtoList.add(toShortDTO(entity));
        });
        return dtoList;
    }

    public List<PlaylistDTO> profilePlaylist(String profileId) {
        ProfileEntity profileEntity = profileService.getById(profileId);

        List<PlaylistDTO> dtoList = new ArrayList<>();

        List<PlaylistEntity> entityList = playlistRepository.findAllByProfileId(profileEntity.getId(),
                Sort.by(Sort.Direction.DESC, "orderNum"));

        entityList.forEach(entity -> {
            dtoList.add(toShortDTO(entity));
        });
        return dtoList;
    }

    public Boolean delete(String playlistId, String profileId) {
        PlaylistEntity entity = getById(playlistId);

        if (!entity.getChannel().getProfileId().toString().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        playlistRepository.delete(entity);

        return true;
    }

    public PlaylistDTO get(String playlistId) {
        PlaylistEntity entity = getById(playlistId);
        return toFullDTO(entity);
    }

    public PlaylistEntity getById(String id) {
        return playlistRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    throw new ItemNotFoundException("Not found!");
                });
    }

    public PlaylistDTO toShortDTO(PlaylistEntity entity) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(entity.getId().toString());
        dto.setName(entity.getName());

        ChannelDTO ChannelDTO = new ChannelDTO(channelService.toOpenUrl(entity.getChannelId().toString()));
        dto.setChannel(ChannelDTO);

        dto.setVideoCount(playlistVideoRepository.getVideoCountByPlaylistId(entity.getId()));


        List<VideoDTO> videoList = playlistVideoRepository.getTop2VideoByPlaylistId(entity.getId())
                .stream()
                .map(playlistVideoEntity -> {
                    VideoDTO videoDTO = new VideoDTO();
                    videoDTO.setId(playlistVideoEntity.getVideoId().toString());
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
        dto.setId(entity.getId().toString());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());

        ChannelDTO channelDTO = channelService.channelShorInfoWithProfile(entity.getChannel());
        dto.setChannel(channelDTO);

        dto.setStatus(entity.getStatus());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

}

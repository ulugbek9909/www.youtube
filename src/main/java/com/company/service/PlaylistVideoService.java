package com.company.service;

import com.company.dto.*;
import com.company.entity.*;
import com.company.exception.AppBadRequestException;
import com.company.exception.AppForbiddenException;
import com.company.exception.ItemNotFoundException;
import com.company.repository.PlaylistRepository;
import com.company.repository.PlaylistVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistVideoService {

    private final PlaylistVideoRepository playlistVideoRepository;
    private final PlaylistRepository playlistRepository;
    private final VideoService videoService;
    private final ChannelService channelService;


    public PlaylistVideoDTO create(PlaylistVideoDTO dto, String profileId) {
        VideoEntity videoEntity = videoService.getById(dto.getVideoId());

        PlaylistEntity playlistEntity = getPlaylistById(dto.getPlaylistId());

        if (!playlistEntity.getChannel().getProfileId().toString().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        PlaylistVideoEntity entity = new PlaylistVideoEntity();
        entity.setPlaylistId(playlistEntity.getId());
        entity.setVideoId(videoEntity.getId());
        entity.setOrderNum(dto.getOrderNum());

        playlistVideoRepository.save(entity);

        entity.setVideo(videoEntity);
        entity.setPlaylist(playlistEntity);
        return toDTO(entity);
    }

    public PlaylistVideoDTO update(UpdateOrderNumDTO dto, String playlistVideoId, String profileId) {
        PlaylistVideoEntity entity = getById(playlistVideoId);

        if (!entity.getPlaylist().getChannel().getProfileId().toString().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        entity.setOrderNum(dto.getOrderNum());
        entity.setUpdatedDate(LocalDateTime.now());

        playlistVideoRepository.save(entity);

        return toDTO(entity);
    }

    public Boolean delete(PlaylistVideoIdDTO dto, String profileId) {
        VideoEntity videoEntity = videoService.getById(dto.getVideoId());

        PlaylistEntity playlistEntity = getPlaylistById(dto.getPlaylistId());

        if (!playlistEntity.getChannel().getProfileId().toString().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        PlaylistVideoEntity entity = getByPlaylistIdAndVideoId(dto.getPlaylistId(), dto.getVideoId());

        playlistVideoRepository.delete(entity);
        return true;
    }

    public List<PlaylistVideoDTO> videosByPlaylistId(String playlistId) {
        PlaylistEntity playlistEntity = getPlaylistById(playlistId);

        List<PlaylistVideoDTO> dtoList = new ArrayList<>();

        List<PlaylistVideoEntity> entityList = playlistVideoRepository.findAllByPlaylistId(playlistEntity.getId(),
                Sort.by(Sort.Direction.ASC, "orderNum"));

        entityList.forEach(entity -> {
            dtoList.add(toDTO(entity));
        });
        return dtoList;
    }

    public PlaylistVideoDTO get(String playlistVideoId) {
        return toDTO(getById(playlistVideoId));
    }

    public PlaylistVideoEntity getByPlaylistIdAndVideoId(String playlistId, String videoId) {
        return playlistVideoRepository
                .findByPlaylistIdAndVideoId(UUID.fromString(playlistId), UUID.fromString(videoId))
                .orElseThrow(() -> {
                    log.warn("Not found playlistId={} videoId={}", playlistId, videoId);
                    return new AppBadRequestException("Not found!");
                });
    }


    public PlaylistEntity getPlaylistById(String id) {
        return playlistRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    throw new ItemNotFoundException("Not found!");
                });
    }

    public PlaylistVideoEntity getById(String id) {
        return playlistVideoRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    throw new ItemNotFoundException("Not found!");
                });

    }

    public PlaylistVideoDTO toDTO(PlaylistVideoEntity entity) {
        PlaylistVideoDTO dto = new PlaylistVideoDTO();
        dto.setId(entity.getId().toString());
        dto.setPlaylistId(entity.getPlaylistId().toString());

        VideoEntity videoEntity = entity.getVideo();
        dto.setVideo(new VideoDTO(videoEntity.getId().toString(),
                videoEntity.getTitle(),
                videoEntity.getDescription(),
                new AttachDTO(videoService.toOpenUrl(entity.getVideoId().toString())),
                videoEntity.getDuration()));

        dto.setChannel(new ChannelDTO(channelService.toOpenUrl(entity.getVideo().getChannelId().toString())));

        dto.setOrderNum(entity.getOrderNum());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }


}

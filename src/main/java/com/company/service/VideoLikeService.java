package com.company.service;

import com.company.dto.ProfileDTO;
import com.company.dto.VideoDTO;
import com.company.dto.VideoLikeDTO;
import com.company.entity.ProfileEntity;
import com.company.entity.VideoEntity;
import com.company.entity.VideoLikeEntity;
import com.company.exception.AppForbiddenException;
import com.company.exception.ItemNotFoundException;
import com.company.repository.VideoLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoLikeService {

    private final VideoLikeRepository videoLikeRepository;
    private final ProfileService profileService;
    private final VideoService videoService;


    public VideoLikeDTO create(VideoLikeDTO dto, Integer profileId) {
        ProfileEntity profileEntity = profileService.getById(profileId);

        VideoEntity videoEntity = videoService.getById(dto.getVideoId());

        Optional<VideoLikeEntity> oldLikeOptional = videoLikeRepository.findByVideoIdAndProfileId(videoEntity.getId(), profileEntity.getId());

        if (oldLikeOptional.isPresent()) {
            VideoLikeEntity entity = oldLikeOptional.get();
            entity.setType(dto.getType());
            videoLikeRepository.save(entity);
            return toDTO(entity);
        }

        VideoLikeEntity entity = new VideoLikeEntity();
        entity.setVideoId(videoEntity.getId());
        entity.setProfileId(profileEntity.getId());
        entity.setType(dto.getType());

        videoLikeRepository.save(entity);

        return toDTO(entity);
    }

    public Boolean delete(Integer likeId, Integer profileId) {
        VideoLikeEntity entity = getById(likeId);

        if (!entity.getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        videoLikeRepository.delete(entity);
        return true;
    }

    public PageImpl<VideoLikeDTO> getByProfileLikedVideo(int page, int size, Integer profileId) {
        ProfileEntity profileEntity = profileService.getById(profileId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        List<VideoLikeDTO> dtoList = new ArrayList<>();

        Page<VideoLikeEntity> entityPage = videoLikeRepository.findAllByProfileId(profileEntity.getId(), pageable);

        entityPage.forEach(entity -> dtoList.add(toDTO(entity)));
        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }

    public VideoLikeDTO get(Integer videoId, Integer profileId) {
        ProfileEntity profileEntity = profileService.getById(profileId);

        VideoEntity videoEntity = videoService.getById(videoId);

        Optional<VideoLikeEntity> optional = videoLikeRepository.findByVideoIdAndProfileId(videoEntity.getId(), profileEntity
                .getId());

        if (optional.isEmpty()) {
            return new VideoLikeDTO();
        }
        return toDTO(optional.get());
    }


    public VideoLikeEntity getById(Integer id) {
        return videoLikeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    return new ItemNotFoundException("Not found!");
                });
    }

    public VideoLikeDTO toDTO(VideoLikeEntity entity) {
        VideoLikeDTO dto = new VideoLikeDTO();
        dto.setId(entity.getId());
        dto.setVideo(new VideoDTO(videoService.toOpenUrl(entity.getVideoId().toString())));
        dto.setProfile(new ProfileDTO(profileService.toOpenUrl(entity.getProfileId())));
        dto.setType(entity.getType());
        return dto;
    }
}

package com.company.service;

import com.company.dto.*;
import com.company.entity.*;
import com.company.enums.ProfileRole;
import com.company.enums.VideoStatus;
import com.company.exception.AppForbiddenException;
import com.company.exception.ItemNotFoundException;
import com.company.mapper.LikeCountSimpleMapper;
import com.company.mapper.ProfileLikesSimpleMapper;
import com.company.repository.PlaylistVideoRepository;
import com.company.repository.VideoLikeRepository;
import com.company.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final ChannelService channelService;
    private final CategoryService categoryService;
    private final AttachService attachService;
    private final ProfileService profileService;
    private final VideoLikeRepository videoLikeRepository;


    @Value("${server.domain.name}")
    private String domainName;


    public VideoDTO create(VideoDTO dto, Integer profileId) {
        ChannelEntity channelEntity = channelService.getById(dto.getChannelId());

        if (!channelEntity.getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        CategoryEntity categoryEntity = categoryService.getById(dto.getCategoryId());

        AttachEntity attachEntity = attachService.getById(dto.getVideoId());

        VideoEntity entity = new VideoEntity();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());

        entity.setStatus(VideoStatus.CREATED);
        entity.setType(dto.getType());

        entity.setCategoryId(categoryEntity.getId());
        entity.setAttachId(attachEntity.getId());
        entity.setChannelId(channelEntity.getId());

        videoRepository.save(entity);

        return toFullDTO(entity);
    }

    public VideoDTO updateAbout(VideoAboutDTO dto, Integer videoId, Integer profileId) {
        VideoEntity entity = getById(videoId);

        if (!entity.getChannel().getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedDate(LocalDateTime.now());

        videoRepository.save(entity);

        return toFullDTO(entity);
    }

    public Boolean changeStatus(Integer videoId, Integer profileId) {
        VideoEntity entity = getById(videoId);

        ProfileEntity profileEntity = profileService.getById(profileId);

        if (entity.getChannel().getProfileId().equals(profileId)
                || profileEntity.getRole().equals(ProfileRole.ADMIN)) {

            switch (entity.getStatus()) {
                case CREATED -> videoRepository.updateStatusAndPublishedDate(VideoStatus.PUBLIC, LocalDateTime.now(), entity.getId());
                case PUBLIC -> videoRepository.updateStatus(VideoStatus.PRIVATE, entity.getId());
                case PRIVATE -> videoRepository.updateStatus(VideoStatus.PUBLIC, entity.getId());
            }
            return true;
        }
        log.warn("Not access {}", profileId);
        throw new AppForbiddenException("Not access!");
    }

    public List<VideoDTO> searchResult(String search) {
        List<VideoDTO> dtoList = new ArrayList<>();

        List<VideoEntity> entityList = videoRepository.findAllByTitleAndStatusAndVisible(search,
                VideoStatus.PUBLIC,
                true,
                Sort.by(Sort.Direction.DESC, "publishedDate"));

        entityList.forEach(entity -> dtoList.add(toShortDTO(entity)));
        return dtoList;
    }

    public Boolean updatePreviewPhoto(VideoPreviewPhotoDTO dto, Integer videoId, Integer profileId) {
        AttachEntity attachEntity = attachService.getById(dto.getPhotoId());

        VideoEntity entity = getById(videoId);

        if (!entity.getChannel().getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        if (Optional.ofNullable(entity.getPreviewAttachId()).isPresent()) {
            if (entity.getPreviewAttachId().equals(dto.getPhotoId())) {
                return true;
            }
            Integer oldAttach = entity.getPreviewAttachId();
            videoRepository.updatePreviewPhoto(attachEntity.getId(), entity.getId());
            attachService.delete(oldAttach);
            return true;
        }
        videoRepository.updatePreviewPhoto(attachEntity.getId(), entity.getAttachId());
        return true;
    }

    public void updateViewCount(Integer videoId) {
        VideoEntity entity = getByIdAndStatus(videoId, VideoStatus.PUBLIC);
        videoRepository.updateViewCount(entity.getId());
    }


    public PageImpl<VideoDTO> paginationByCategoryId(int page, int size, Integer categoryId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedDate"));

        CategoryEntity categoryEntity = categoryService.getById(categoryId);

        List<VideoDTO> dtoList = new ArrayList<>();

        Page<VideoEntity> entityPage = videoRepository.findAllByCategoryIdAndStatusAndVisible(categoryEntity.getId(),
                VideoStatus.PUBLIC,
                true,
                pageable);

        entityPage.forEach(entity -> dtoList.add(toShortDTO(entity)));
        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }

    /*public PageImpl<VideoDTO> paginationByTagId(int page, int size, String tagId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedDate"));

        TagEntity tagEntity = tagService.get(tagId);

        List<VideoDTO> dtoList = new ArrayList<>();

        Page<VideoEntity> entityPage = videoRepository.findAllByTagIdAndStatus(tagEntity.getId(),
                VideoStatus.PUBLIC,
                pageable);

        entityPage.forEach(entity -> {
            dtoList.add(toDTO(entity));
        });
        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }*/
    public PageImpl<VideoPlaylistDTO> pagination(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        List<VideoDTO> dtoVideoList = new ArrayList<>();
        List<PlaylistDTO> dtoPlaylist = new ArrayList<>();
        List<VideoPlaylistDTO> dtoVideoPlaylist = new ArrayList<>();

        Page<VideoEntity> entityPage = videoRepository.findAll(pageable);

        entityPage.forEach(entity -> {
            PlaylistVideoEntity playlistVideoEntity = getByVideoId(entity.getId());
            if (Optional.ofNullable(playlistVideoEntity).isPresent()) {
                dtoPlaylist.add(toShortPlaylistDTO(playlistVideoEntity.getPlaylist()));
            }
            dtoVideoList.add(toShortDTO(entity));
        });
        dtoVideoPlaylist.add(new VideoPlaylistDTO(dtoVideoList, dtoPlaylist));

        return new PageImpl<>(dtoVideoPlaylist, pageable, entityPage.getTotalElements());
    }

    public PageImpl<VideoDTO> paginationByChannelId(int page, int size, Integer channelId) {
        ChannelEntity channelEntity = channelService.getById(channelId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        List<VideoDTO> dtoList = new ArrayList<>();

        Page<VideoEntity> entityPage = videoRepository.findAllByChannelIdAndStatusAndVisible(channelEntity.getId(),
                VideoStatus.PUBLIC,
                true,
                pageable);

        entityPage.forEach(entity -> dtoList.add(toShortDTO(entity)));

        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }

    public Boolean delete(Integer videoId, Integer profileId) {
        VideoEntity entity = getById(videoId);

        if (!entity.getChannel().getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        videoRepository.updateVisible(entity.getId());

        attachService.delete(entity.getAttachId());

        if (Optional.ofNullable(entity.getPreviewAttachId()).isPresent()) {
            attachService.delete(entity.getPreviewAttachId());
        }

        return true;
    }

    public VideoDTO get(Integer id, Integer profileId) {
        VideoEntity entity = getById(id);

        ProfileEntity profileEntity = profileService.getById(profileId);

        if (!entity.getChannel().getProfileId().equals(profileId)
                && entity.getStatus().equals(VideoStatus.PUBLIC)) {
            return toFullDTO(entity);
        }

        if (entity.getChannel().getProfileId().equals(profileId)
                || profileEntity.getRole().equals(ProfileRole.ADMIN)) {
            return toFullDTO(entity);
        }

        log.warn("Not access {}", profileId);
        throw new AppForbiddenException("Not access!");
    }

    public VideoEntity getById(Integer id) {
        return videoRepository.findByIdAndVisible(id, true)
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    throw new ItemNotFoundException("Not found!");
                });
    }

    public VideoEntity getByIdAndStatus(Integer id, VideoStatus status) {
        return videoRepository.findByIdAndStatusAndVisible(id, status, true)
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    throw new ItemNotFoundException("Not found!");
                });
    }

    public PlaylistVideoEntity getByVideoId(Integer videoId) {
        return playlistVideoRepository
                .findByVideoId(videoId)
                .orElse(null);
    }

    public PlaylistDTO toShortPlaylistDTO(PlaylistEntity entity) {
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
                    videoDTO.setUrl(toOpenUrl(playlistVideoEntity.getVideoId().toString()));
                    videoDTO.setDuration(playlistVideoEntity.getVideo().getDuration());
                    return videoDTO;
                }).toList();

        dto.setVideoList(videoList);
        dto.setCreatedDate(entity.getCreatedDate());
        return dto;
    }

    public VideoDTO toShortDTO(VideoEntity entity) {
        VideoDTO dto = new VideoDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());

        dto.setViewCount(entity.getViewCount());

        dto.setChannel(new ChannelDTO(channelService.toOpenUrl(entity.getChannelId().toString())));

        dto.setVideo(new AttachDTO(attachService.toOpenUrl(entity.getAttachId().toString())));

        if (Optional.ofNullable(entity.getPreviewAttachId()).isPresent()) {
            dto.setPreviewPhoto(new AttachDTO(attachService.toOpenUrl(entity.getPreviewAttachId().toString())));
        }

        dto.setDuration(entity.getDuration());
        dto.setPublishedDate(dto.getPublishedDate());
        return dto;
    }

    public VideoDTO toFullDTO(VideoEntity entity) {
        VideoDTO dto = new VideoDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());

        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setSharedCount(entity.getSharedCount());
        dto.setViewCount(entity.getViewCount());

        dto.setChannel(new ChannelDTO(channelService.toOpenUrl(entity.getChannelId().toString())));

        dto.setVideo(new AttachDTO(attachService.toOpenUrl(entity.getAttachId().toString())));

        if (Optional.ofNullable(entity.getPreviewAttachId()).isPresent()) {
            dto.setPreviewPhoto(new AttachDTO(attachService.toOpenUrl(entity.getPreviewAttachId().toString())));
        }

        dto.setCategory(new CategoryDTO(categoryService.toOpenUrl(entity.getCategoryId().toString())));

        dto.setLikes(getLikesCountByVideoId(entity.getId()));
        dto.setProfileLikes(getProfileLikesByVideoId(entity.getId()));

        dto.setDuration(entity.getDuration());
        dto.setPublishedDate(dto.getPublishedDate());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    public VideoLikeDTO getLikesCountByVideoId(Integer videoId) {
        LikeCountSimpleMapper mapper = videoLikeRepository.getLikeCountByVideoId(videoId);
        return new VideoLikeDTO(mapper.getLike_count(), mapper.getDislike_count());
    }

    public List<VideoLikeDTO> getProfileLikesByVideoId(Integer videoId) {
        List<ProfileLikesSimpleMapper> mapper = videoLikeRepository.getProfileLikesByVideoId(videoId);

        List<VideoLikeDTO> dtoList = new ArrayList<>();

        mapper.forEach(entity -> dtoList.add(new VideoLikeDTO(new ProfileDTO(profileService.toOpenUrl(entity.getProfile_id())), entity.getType())));
        return dtoList;
    }

    public String toOpenUrl(String id) {
        return domainName + "video/public/" + id;
    }

}

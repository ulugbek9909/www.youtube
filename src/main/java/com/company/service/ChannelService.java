package com.company.service;

import com.company.dto.AttachDTO;
import com.company.dto.ChannelAboutDTO;
import com.company.dto.ChannelDTO;
import com.company.dto.ProfileDTO;
import com.company.entity.AttachEntity;
import com.company.entity.ChannelEntity;
import com.company.entity.ProfileEntity;
import com.company.enums.ChannelStatus;
import com.company.enums.ProfileRole;
import com.company.exception.AppBadRequestException;
import com.company.exception.AppForbiddenException;
import com.company.exception.ItemNotFoundException;
import com.company.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {


    private final ChannelRepository channelRepository;
    private final AttachService attachService;
    private final ProfileService profileService;

    @Value("${server.domain.name}")
    private String domainName;


    public ChannelDTO create(ChannelDTO dto, Integer profileId) {
        ChannelEntity entity = new ChannelEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStatus(ChannelStatus.ACTIVE);
        entity.setProfileId(profileId);

        try {
            channelRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.warn("Unique {}", dto);
            throw new AppBadRequestException("Unique!");
        }
        return toDTO(entity);
    }

    public ChannelDTO updateAbout(ChannelAboutDTO dto, Integer channelId, Integer profileId) {
        ChannelEntity entity = getById(channelId);

        if (!entity.getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedDate(LocalDateTime.now());
        try {
            channelRepository.updateBio(dto.getName(), dto.getDescription(), LocalDateTime.now(), entity.getId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Unique {}", dto);
            throw new AppBadRequestException("Unique!");
        }
        return toDTO(entity);
    }

    public Boolean channelImage(Integer attachId, Integer channelId, Integer profileId) {
        AttachEntity attachEntity = attachService.getById(attachId);

        ChannelEntity entity = getById(channelId);

        if (!entity.getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        if (Optional.ofNullable(entity.getPhotoId()).isPresent()) {
            if (entity.getPhotoId().equals(attachId)) {
                return true;
            }
            Integer oldAttach = entity.getPhotoId();
            channelRepository.updatePhoto(attachId, channelId);
            attachService.delete(oldAttach);
            return true;
        }
        channelRepository.updatePhoto(attachId, channelId);
        return true;
    }


    public Boolean channelBanner(Integer attachId, Integer channelId, Integer profileId) {
        AttachEntity attachEntity = attachService.getById(attachId);

        ChannelEntity entity = getById(channelId);

        if (!entity.getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        if (Optional.ofNullable(entity.getBannerId()).isPresent()) {
            if (entity.getBannerId().equals(attachId)) {
                return true;
            }
            Integer oldAttach = entity.getBannerId();
            channelRepository.updateBanner(attachId, channelId);
            attachService.delete(oldAttach);
            return true;
        }
        channelRepository.updateBanner(attachId, channelId);
        return true;
    }

    public PageImpl<ChannelDTO> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        List<ChannelDTO> dtoList = new ArrayList<>();

        Page<ChannelEntity> entityPage = channelRepository.findAll(pageable);

        entityPage.forEach(entity -> dtoList.add(toDTO(entity)));
        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }

    public Boolean changeStatus(Integer channelId, Integer profileId) {
        ChannelEntity entity = getById(channelId);

        ProfileEntity profileEntity = profileService.getById(profileId);

        if (entity.getProfileId().equals(profileId) || profileEntity.getRole().equals(ProfileRole.ADMIN)) {
            switch (entity.getStatus()) {
                case ACTIVE -> channelRepository.updateStatus(ChannelStatus.BLOCK, entity.getId());
                case BLOCK -> channelRepository.updateStatus(ChannelStatus.ACTIVE, entity.getId());
            }

            return true;
        }
        log.warn("Not access {}", profileId);
        throw new AppForbiddenException("Not access!");
    }

    public List<ChannelDTO> profileChannelList(Integer profileId) {
        ProfileEntity profileEntity = profileService.getById(profileId);

        List<ChannelDTO> dtoList = new ArrayList<>();

        List<ChannelEntity> entityList = channelRepository.findAllByProfileId(profileEntity.getId(),
                Sort.by(Sort.Direction.ASC, "name"));

        entityList.forEach(entity -> dtoList.add(toDTO(entity)));
        return dtoList;
    }

    public Boolean delete(Integer channelId, Integer profileId) {
        ChannelEntity entity = getById(channelId);

        if (!entity.getProfileId().equals(profileId)) {
            log.warn("Not access {}", profileId);
            throw new AppForbiddenException("Not access!");
        }

        channelRepository.delete(entity);

        attachService.delete(entity.getBannerId());
        attachService.delete(entity.getPhotoId());

        return true;
    }

    public ChannelDTO get(Integer id) {
        ChannelEntity entity = getById(id);
        return toDTO(entity);
    }

    public ChannelEntity getById(Integer id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    throw new ItemNotFoundException("Not found!");
                });
    }

    public ChannelDTO toDTO(ChannelEntity entity) {
        ChannelDTO dto = new ChannelDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        if (Optional.ofNullable(entity.getPhotoId()).isPresent()) {
            AttachDTO attachDTO = new AttachDTO(attachService.toOpenUrl(entity.getPhotoId().toString()));
            dto.setPhoto(attachDTO);
        }
        if (Optional.ofNullable(entity.getBannerId()).isPresent()) {
            AttachDTO attachDTO = new AttachDTO(attachService.toOpenUrl(entity.getBannerId().toString()));
            dto.setBanner(attachDTO);
        }
        ProfileDTO profileDTO = new ProfileDTO(profileService.toOpenUrl(entity.getProfileId()));
        dto.setProfile(profileDTO);

        dto.setStatus(entity.getStatus());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    public String toOpenUrl(String id) {
        return domainName + "channel/" + id;
    }


}

package com.company.service;

import com.company.dto.*;
import com.company.entity.AttachEntity;
import com.company.entity.ProfileEntity;
import com.company.enums.EmailType;
import com.company.enums.ProfileStatus;
import com.company.exception.AppBadRequestException;
import com.company.exception.AppNotAcceptableException;
import com.company.exception.ItemAlreadyExistsException;
import com.company.exception.ItemNotFoundException;
import com.company.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final AttachService attachService;
    private final AuthService authService;

    @Value("${server.domain.name}")
    private String domainName;


    public ProfileDTO create(ProfileDTO dto) {
        checkEmail(dto.getEmail());

        ProfileEntity entity = new ProfileEntity();
        entity.setName(dto.getName());
        entity.setSurname(dto.getSurname());
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole());
        entity.setStatus(ProfileStatus.ACTIVE);

        String password = DigestUtils.md5Hex(dto.getPassword());
        entity.setPassword(password);

        profileRepository.save(entity);
        return toDTO(entity);
    }

    public PageImpl<ProfileDTO> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        List<ProfileDTO> dtoList = new ArrayList<>();

        Page<ProfileEntity> entityPage = profileRepository.findAll(pageable);
        entityPage.forEach(entity -> dtoList.add(toDTO(entity)));

        return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }

    public ProfileDTO updateBio(Integer id, ProfileBioDTO dto) {
        ProfileEntity entity = getById(id);

        profileRepository.updateBio(dto.getName(), dto.getSurname(), LocalDateTime.now(), entity.getId());

        return get(id);
    }

    public Boolean delete(Integer id) {
        ProfileEntity entity = getById(id);
        profileRepository.delete(entity);
        return true;
    }

    public Boolean profileImage(Integer attachId, Integer pId) {
        AttachEntity attachEntity = attachService.getById(attachId);

        ProfileEntity entity = getById(pId);

        if (Optional.ofNullable(entity.getAttach()).isPresent()) {
            if (entity.getAttachId().equals(attachId)) {
                return true;
            }
            Integer oldAttach = entity.getAttachId();
            profileRepository.updateAttach(attachId, pId);
            attachService.delete(oldAttach);
            return true;
        }
        profileRepository.updateAttach(attachId, pId);
        return true;
    }

    public String emailReset(ProfileEmailDTO dto, Integer profileId) {
        ProfileEntity entity = getById(profileId);

        checkEmail(dto.getEmail());

        entity.setEmail(dto.getEmail());

        Thread thread = new Thread(() -> authService.sendEmail(entity, "profile/email", EmailType.RESET));
        thread.start();

        return "Confirm your email address.\nCheck your email!";
    }

    public String emailConfirm(Integer profileId, String email) {
        ProfileEntity entity = getById(profileId);
        try {
            profileRepository.updateEmail(email, entity.getId());
            return "Successfully changed";
        } catch (DataIntegrityViolationException e) {
            log.warn("Unsuccessfully changed {}", email);
            throw new AppNotAcceptableException("Unsuccessfully changed!");
        }
    }

    public String changePassword(ProfilePasswordDTO dto, Integer profileId) {
        ProfileEntity entity = getById(profileId);

        if (!entity.getPassword().equals(DigestUtils.md5Hex(dto.getOldPassword()))) {
            log.warn("Invalid Old Password {}", profileId);
            throw new AppBadRequestException("Invalid Old Password");
        }

        String password = DigestUtils.md5Hex(dto.getNewPassword());
        entity.setPassword(password);

        try {
            profileRepository.updatePassword(password, entity.getId());
            return "Successfully changed";
        } catch (DataIntegrityViolationException e) {
            log.warn("Unsuccessfully changed {}", profileId);
            throw new AppNotAcceptableException("Unsuccessfully changed!");
        }
    }

    public ProfileEntity getById(Integer id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Not found {}", id);
                    return new ItemNotFoundException("Not Found!");
                });
    }

    public void checkEmail(String email) {
        profileRepository.findByEmail(email)
                .ifPresent(profileEntity -> {
                    log.warn("Unique {}", email);
                    throw new ItemAlreadyExistsException("This Email already used!");
                });
    }

    public ProfileDTO get(Integer id) {
        ProfileEntity entity = getById(id);

        ProfileDTO dto = new ProfileDTO();
        dto.setName(entity.getName());
        dto.setSurname(entity.getSurname());
        dto.setEmail(entity.getEmail());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());

        if (Optional.ofNullable(entity.getAttach()).isPresent()) {
            AttachDTO attachDTO = new AttachDTO(attachService.toOpenUrl(entity.getAttach().getId().toString()));
            dto.setImage(attachDTO);
        }

        return dto;
    }

    public ProfileDTO toDTO(ProfileEntity entity) {
        ProfileDTO dto = new ProfileDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSurname(entity.getSurname());
        dto.setEmail(entity.getEmail());
        dto.setRole(entity.getRole());
        dto.setStatus(dto.getStatus());

        if (Optional.ofNullable(entity.getAttach()).isPresent()) {
            AttachDTO attachDTO = new AttachDTO(attachService.toOpenUrl(entity.getAttach().getId().toString()));
            dto.setImage(attachDTO);
        }

        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    public String toOpenUrl(Integer id) {
        return domainName + "profile/" + id;
    }
}

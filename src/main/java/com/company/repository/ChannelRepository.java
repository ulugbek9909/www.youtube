package com.company.repository;

import com.company.entity.ChannelEntity;
import com.company.enums.ChannelStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface ChannelRepository extends JpaRepository<ChannelEntity, Integer> {

    @Transactional
    @Modifying
    @Query("update ChannelEntity set photoId = :attachId where id = :id")
    void updatePhoto(@Param("attachId") Integer attachId, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update ChannelEntity set bannerId = :attachId where id = :id")
    void updateBanner(@Param("attachId") Integer attachId, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update ChannelEntity set name = :name , description = :description , updatedDate = :now where id = :id")
    void updateBio(@Param("name") String name, @Param("description") String description,
                  @Param("now") LocalDateTime now, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update ChannelEntity set status = :status where id = :id")
    void updateStatus(@Param("status") ChannelStatus status, @Param("id") Integer id);

    List<ChannelEntity> findAllByProfileId(Integer profileId, Sort sort);

}
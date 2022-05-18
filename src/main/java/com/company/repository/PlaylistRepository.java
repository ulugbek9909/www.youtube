package com.company.repository;

import com.company.entity.PlaylistEntity;
import com.company.enums.PlaylistStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<PlaylistEntity, Integer> {

    @Transactional
    @Modifying
    @Query("update PlaylistEntity set status = :status where id = :id")
    void updateStatus(@Param("status") PlaylistStatus status, @Param("id") Integer id);

    List<PlaylistEntity> findAllByChannelIdAndStatus(Integer id, PlaylistStatus status, Sort sort);

    @Query("from PlaylistEntity where channel.profileId = :profileId")
    List<PlaylistEntity> findAllByProfileId(@Param("profileId") Integer profileId, Sort sort);

}
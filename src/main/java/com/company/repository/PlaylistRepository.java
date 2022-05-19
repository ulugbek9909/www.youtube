package com.company.repository;

import com.company.entity.PlaylistEntity;
import com.company.enums.PlaylistStatus;
import com.company.mapper.PlayListInfoAdminMapper;
import com.company.mapper.PlayListInfoJpqlAdminMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface PlaylistRepository extends JpaRepository<PlaylistEntity, UUID> {

    @Transactional
    @Modifying
    @Query("update PlaylistEntity set status = :status where id = :id")
    int updateStatus(@Param("status") PlaylistStatus status, @Param("id") UUID id);

    List<PlaylistEntity> findAllByChannelIdAndStatus(UUID id, PlaylistStatus status, Sort sort);

    @Query("from PlaylistEntity where channel.profileId = :profileId")
    List<PlaylistEntity> findAllByProfileId(@Param("profileId") UUID profileId, Sort sort);

    @Query(value = "select  cast(pl.id as varchar) as pl_id,pl.name as pl_name,pl.description as pl_description,pl.status as pl_status,pl.order_num, " +
            "cast(ch.id as varchar) as ch_id, ch.name as ch_name,cast(ch.photo_id as varchar) as ch_photo_id, " +
            "cast(pr.id as varchar) as pr_id,pr.name as pr_name,pr.surname as pr_surname, cast(pr.attach_id as varchar) as pr_photo_id " +
            "from playlist as pl " +
            "inner join channel as ch on pl.channel_id = ch.id " +
            "inner join profile as pr on pr.id = ch.profile_id " +
            "order by pl.created_date", nativeQuery = true)
    public Page<PlayListInfoAdminMapper> getPlaylistInfoForAdmin(Pageable pageable);

    @Query("SELECT pl.id as pl_id,pl.name as pl_name,pl.description as pl_description,pl.status as pl_status,pl.orderNum as pl_order_num, " +
            "ch.id as ch_id, ch.name as ch_name, ch.photoId as ch_photo_id, " +
            "pr.id as pr_id, pr.name as pr_name,pr.surname as pr_surname,pr.attachId as pr_photo_id " +
            "FROM PlaylistEntity  as pl " +
            " INNER JOIN pl.channel as ch " +
            " INNER JOIN ch.profile as pr " +
            " order by pl.createdDate desc ")
    public Page<PlayListInfoJpqlAdminMapper> getPlaylistInfoJpql(Pageable pageable);

}
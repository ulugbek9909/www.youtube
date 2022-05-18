package com.company.repository;

import com.company.entity.VideoEntity;
import com.company.enums.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<VideoEntity, Integer> {


    @Transactional
    @Modifying
    @Query("update VideoEntity set status = :status where id = :id")
    void updateStatus(@Param("status") VideoStatus status, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update VideoEntity set status = :status, publishedDate = :publishedDate where id = :id")
    void updateStatusAndPublishedDate(@Param("status") VideoStatus status,
                                     @Param("publishedDate") LocalDateTime publishedDate,
                                     @Param("id") Integer id);

    List<VideoEntity> findAllByTitleAndStatusAndVisible(String title, VideoStatus status, Boolean visible, Sort sort);

    Page<VideoEntity> findAllByCategoryIdAndStatusAndVisible(Integer categoryId, VideoStatus status, Boolean visible, Pageable pageable);

    Page<VideoEntity> findAllByChannelIdAndStatusAndVisible(Integer channelId, VideoStatus status, Boolean visible, Pageable pageable);


    Optional<VideoEntity> findByIdAndVisible(Integer id, Boolean visible);

    @Transactional
    @Modifying
    @Query(value = "update VideoEntity set viewCount = viewCount + 1 where id =:id")
    void updateViewCount(@Param("id") Integer id);

    Optional<VideoEntity> findByIdAndStatusAndVisible(Integer id, VideoStatus status, Boolean visible);

    @Transactional
    @Modifying
    @Query(value = "update VideoEntity set visible = false where id =:id")
    void updateVisible(@Param("id") Integer id);

    @Transactional
    @Modifying
    @Query(value = "update VideoEntity set previewAttachId = :attachId where id =:id")
    void updatePreviewPhoto(@Param("attachId") Integer attachId, Integer id);
}
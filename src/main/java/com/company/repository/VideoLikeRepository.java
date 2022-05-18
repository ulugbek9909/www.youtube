package com.company.repository;

import com.company.entity.VideoLikeEntity;
import com.company.mapper.LikeCountSimpleMapper;
import com.company.mapper.ProfileLikesSimpleMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoLikeRepository extends JpaRepository<VideoLikeEntity, Integer> {

    Page<VideoLikeEntity> findAllByProfileId(Integer profileId, Pageable pageable);

    Optional<VideoLikeEntity> findByVideoIdAndProfileId(Integer videoId, Integer profileId);

    @Query(value = "select sum(case when type = 'LIKE' THEN 1 else 0 END) like_count," +
            "sum(case when type = 'LIKE' THEN 0 else 1 END) dislike_count " +
            "from video_like " +
            "where video_id = :videoId", nativeQuery = true)
    LikeCountSimpleMapper getLikeCountByVideoId(@Param("videoId") Integer videoId);

    @Query(value = "select CAST(profile_id as varchar) profile_id,type " +
            "from video_like " +
            "where video_id = :videoId " +
            "group by type, profile_id", nativeQuery = true)
    List<ProfileLikesSimpleMapper> getProfileLikesByVideoId(@Param("videoId") Integer videoId);
}
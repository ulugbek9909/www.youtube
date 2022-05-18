package com.company.entity;

import com.company.enums.VideoStatus;
import com.company.enums.VideoType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video")
@Getter
@Setter
public class VideoEntity extends BaseEntity {

    @Column
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private CategoryEntity category;

    @Column(name = "attach_id", nullable = false)
    private UUID attachId;

    @Column(name = "preview_attach_id")
    private UUID previewAttachId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", insertable = false, updatable = false)
    private ChannelEntity channel;

    @Column
    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    private VideoType type;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "shared_count")
    private Integer sharedCount = 0;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    @Column
    private Long duration;

    @Column
    private Boolean visible = true;

}

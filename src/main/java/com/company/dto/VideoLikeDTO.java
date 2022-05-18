package com.company.dto;

import com.company.enums.LikeType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class VideoLikeDTO extends BaseDTO {

    private Integer profileId;
    private ProfileDTO profile;

    @NotBlank(message = "VideoId required")
    private Integer videoId;
    private VideoDTO video;

    @NotNull(message = "Type not be null")
    private LikeType type;

    private Integer likeCount;
    private Integer dislikeCount;

    public VideoLikeDTO(ProfileDTO profile, LikeType type) {
        this.profile = profile;
        this.type = type;
    }

    public VideoLikeDTO(Integer likeCount, Integer dislikeCount) {
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
    }
}

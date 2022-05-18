package com.company.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaylistVideoDTO extends BaseDTO {

    @NotBlank(message = "VideoId required")
    private Integer videoId;
    private VideoDTO video;

    @NotBlank(message = "PlaylistId required")
    private Integer playlistId;
    private PlaylistDTO playlist;

    @NotNull(message = "OrderNum required")
    @Positive(message = "Invalid OrderNum")
    private Integer orderNum;

    private ChannelDTO channel;

}

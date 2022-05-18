package com.company.dto;

import com.company.enums.ChannelStatus;
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
public class ChannelDTO extends BaseDTO {

    @NotBlank(message = "Name required")
    private String name;

    @NotNull(message = "Description not be null")
    private String description;

    private Integer photoId;
    private AttachDTO photo;

    private ChannelStatus status;

    private Integer bannerId;
    private AttachDTO banner;

    private Integer profileId;
    private ProfileDTO profile;

    private String url;

    public ChannelDTO(String url) {
        this.url = url;
    }
}

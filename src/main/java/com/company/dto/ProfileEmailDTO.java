package com.company.dto;

import com.company.annotation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileEmailDTO {

    @NotBlank(message = "Email required")
    @ValidEmail
    private String email;

}

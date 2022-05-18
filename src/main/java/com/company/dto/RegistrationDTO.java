package com.company.dto;

import javax.validation.constraints.Size;
import javax.validation.constraints.NotBlank;

import com.company.annotation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrationDTO {

    @NotBlank(message = "Name required")
    private String name;

    @NotBlank(message = "Surname required")
    private String surname;

    @ValidEmail
    private String email;

    @NotBlank(message = "Password required")
    @Size(min = 8, message = "Password length must be between 8 or greater than")
    private String password;

}

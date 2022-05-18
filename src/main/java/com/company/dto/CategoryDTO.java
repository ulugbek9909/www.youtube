package com.company.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class CategoryDTO extends BaseDTO {

    @NotBlank(message = "Name required")
    private String name;


    private String url;

    public CategoryDTO(String url) {
        this.url = url;
    }
}

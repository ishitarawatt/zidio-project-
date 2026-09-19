package com.zidio.keystone.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDto {
    private Long id;

    @NotBlank
    private String name;

    private String contactEmail;
}

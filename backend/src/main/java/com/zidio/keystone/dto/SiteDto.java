package com.zidio.keystone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteDto {
    private Long id;

    @NotNull
    private Long customerId;

    @NotBlank
    private String name;

    private String address;
}

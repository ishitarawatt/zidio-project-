package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkOrderRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Priority priority;

    @NotNull
    private Long customerId;

    @NotNull
    private Long siteId;
}

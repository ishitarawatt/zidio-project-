package com.zidio.keystone.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartUsageRequest {
    @NotNull
    private Long partId;

    @Min(1)
    private Integer qtyUsed;
}

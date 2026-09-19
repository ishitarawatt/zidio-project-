package com.zidio.keystone.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimeLogRequest {
    @Min(1)
    private Integer minutes;

    private String note;
}

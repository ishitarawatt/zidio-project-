package com.zidio.keystone.dto;

import com.zidio.keystone.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusChangeRequest {
    @NotNull
    private WorkOrderStatus toStatus;

    private String note;
}

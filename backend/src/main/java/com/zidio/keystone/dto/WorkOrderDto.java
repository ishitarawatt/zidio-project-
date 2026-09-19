package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Priority;
import com.zidio.keystone.domain.WorkOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class WorkOrderDto {
    private Long id;
    private String code;
    private String title;
    private String description;
    private Priority priority;
    private WorkOrderStatus status;
    private Instant slaDueAt;
    private boolean slaBreached;
    private Long customerId;
    private String customerName;
    private Long siteId;
    private String siteName;
    private Long assignedToId;
    private String assignedToName;
    private Instant createdAt;
    private Instant updatedAt;
}

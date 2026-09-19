package com.zidio.keystone.controller;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.exception.ApiException;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<WorkOrderDto> list(Pageable pageable, Authentication auth) {
        User current = currentUser(auth);
        Page<WorkOrder> page;

        // Server-side scoping: a customer only ever sees their own org's work orders,
        // a technician only sees jobs assigned to them.
        if (current.getRole() == Role.CUSTOMER) {
            page = workOrderService.listForCustomer(current.getCustomer().getId(), pageable);
        } else if (current.getRole() == Role.TECHNICIAN) {
            page = workOrderService.listForTechnician(current.getId(), pageable);
        } else {
            page = workOrderService.list(pageable);
        }
        return page.map(this::toDto);
    }

    @GetMapping("/{id}")
    public WorkOrderDto get(@PathVariable Long id, Authentication auth) {
        WorkOrder wo = workOrderService.get(id);
        enforceReadAccess(wo, currentUser(auth));
        return toDto(wo);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<WorkOrderDto> create(@Valid @RequestBody CreateWorkOrderRequest req, Authentication auth) {
        User current = currentUser(auth);
        // Customers may only raise requests for their own organisation.
        if (current.getRole() == Role.CUSTOMER
                && (current.getCustomer() == null || !current.getCustomer().getId().equals(req.getCustomerId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only raise requests for your own organisation");
        }
        return ResponseEntity.ok(toDto(workOrderService.create(req)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public WorkOrderDto assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        return toDto(workOrderService.assign(id, req.getTechnicianId()));
    }

    @PostMapping("/{id}/status")
    public WorkOrderDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest req) {
        return toDto(workOrderService.changeStatus(id, req.getToStatus(), req.getNote()));
    }

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER')")
    public ResponseEntity<Void> logParts(@PathVariable Long id, @Valid @RequestBody PartUsageRequest req) {
        workOrderService.logPartUsage(id, req.getPartId(), req.getQtyUsed());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/time")
    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER')")
    public ResponseEntity<Void> logTime(@PathVariable Long id, @Valid @RequestBody TimeLogRequest req) {
        workOrderService.logTime(id, req.getMinutes(), req.getNote());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/history")
    public Object history(@PathVariable Long id, Authentication auth) {
        WorkOrder wo = workOrderService.get(id);
        enforceReadAccess(wo, currentUser(auth));
        return workOrderService.history(id);
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public DashboardSummaryDto summary() {
        return workOrderService.summary();
    }

    // ---------- helpers ----------

    private void enforceReadAccess(WorkOrder wo, User current) {
        if (current.getRole() == Role.CUSTOMER) {
            if (current.getCustomer() == null || !wo.getCustomer().getId().equals(current.getCustomer().getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view another organisation's work order");
            }
        } else if (current.getRole() == Role.TECHNICIAN) {
            if (wo.getAssignedTo() == null || !wo.getAssignedTo().getId().equals(current.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You can only view jobs assigned to you");
            }
        }
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private WorkOrderDto toDto(WorkOrder w) {
        return new WorkOrderDto(
                w.getId(), w.getCode(), w.getTitle(), w.getDescription(), w.getPriority(), w.getStatus(),
                w.getSlaDueAt(), w.isSlaBreached(), w.getCustomer().getId(), w.getCustomer().getName(),
                w.getSite().getId(), w.getSite().getName(),
                w.getAssignedTo() != null ? w.getAssignedTo().getId() : null,
                w.getAssignedTo() != null ? w.getAssignedTo().getName() : null,
                w.getCreatedAt(), w.getUpdatedAt()
        );
    }
}

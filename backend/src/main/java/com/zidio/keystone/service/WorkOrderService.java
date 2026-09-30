package com.zidio.keystone.service;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.*;
import com.zidio.keystone.exception.ApiException;
import com.zidio.keystone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final PartRepository partRepository;
    private final PartUsageRepository partUsageRepository;
    private final TimeLogRepository timeLogRepository;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> TRANSITIONS = new EnumMap<>(WorkOrderStatus.class);
    static {
        TRANSITIONS.put(WorkOrderStatus.NEW, Set.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.CANCELLED));
        TRANSITIONS.put(WorkOrderStatus.ASSIGNED, Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED));
        TRANSITIONS.put(WorkOrderStatus.IN_PROGRESS, Set.of(WorkOrderStatus.ON_HOLD, WorkOrderStatus.COMPLETED));
        TRANSITIONS.put(WorkOrderStatus.ON_HOLD, Set.of(WorkOrderStatus.IN_PROGRESS));
        TRANSITIONS.put(WorkOrderStatus.COMPLETED, Set.of(WorkOrderStatus.CLOSED, WorkOrderStatus.IN_PROGRESS));
        TRANSITIONS.put(WorkOrderStatus.CLOSED, Set.of());
        TRANSITIONS.put(WorkOrderStatus.CANCELLED, Set.of());
    }

    private static final Map<WorkOrderStatus, Set<Role>> TRANSITION_ROLES = new EnumMap<>(WorkOrderStatus.class);
    static {
        TRANSITION_ROLES.put(WorkOrderStatus.ASSIGNED, Set.of(Role.DISPATCHER, Role.MANAGER));
        TRANSITION_ROLES.put(WorkOrderStatus.IN_PROGRESS, Set.of(Role.TECHNICIAN, Role.MANAGER));
        TRANSITION_ROLES.put(WorkOrderStatus.ON_HOLD, Set.of(Role.TECHNICIAN, Role.MANAGER));
        TRANSITION_ROLES.put(WorkOrderStatus.COMPLETED, Set.of(Role.TECHNICIAN, Role.MANAGER));
        TRANSITION_ROLES.put(WorkOrderStatus.CLOSED, Set.of(Role.MANAGER));
        TRANSITION_ROLES.put(WorkOrderStatus.CANCELLED, Set.of(Role.DISPATCHER, Role.MANAGER));
    }

    private static final Map<Priority, Duration> SLA_WINDOWS = Map.of(
            Priority.CRITICAL, Duration.ofHours(4),
            Priority.HIGH, Duration.ofHours(24),
            Priority.MEDIUM, Duration.ofHours(72),
            Priority.LOW, Duration.ofHours(168)
    );

    @Transactional(readOnly = true)
    public Page<WorkOrder> list(Pageable pageable) {
        return workOrderRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrder> listForCustomer(Long customerId, Pageable pageable) {
        return workOrderRepository.findByCustomerId(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrder> listForTechnician(Long technicianId, Pageable pageable) {
        return workOrderRepository.findByAssignedToId(technicianId, pageable);
    }

    @Transactional(readOnly = true)
    public WorkOrder get(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Work order not found"));
    }

    @Transactional(readOnly = true)
    public List<WorkOrderStatusHistory> history(Long workOrderId) {
        return historyRepository.findByWorkOrderIdOrderByChangedAtAsc(workOrderId);
    }

    @Transactional
    public WorkOrder create(CreateWorkOrderRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Customer not found"));
        Site site = siteRepository.findById(req.getSiteId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Site not found"));

        if (!site.getCustomer().getId().equals(customer.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Site does not belong to the given customer");
        }

        WorkOrder wo = new WorkOrder();
        wo.setTitle(req.getTitle());
        wo.setDescription(req.getDescription());
        wo.setPriority(req.getPriority());
        wo.setCustomer(customer);
        wo.setSite(site);
        wo.setStatus(WorkOrderStatus.NEW);
        wo.setCode(generateCode());
        wo.setSlaDueAt(Instant.now().plus(SLA_WINDOWS.get(req.getPriority())));

        WorkOrder saved = workOrderRepository.save(wo);
        writeHistory(saved, null, WorkOrderStatus.NEW, "Created");
        return saved;
    }

    @Transactional
    public WorkOrder assign(Long workOrderId, Long technicianId) {
        WorkOrder wo = get(workOrderId);
        if (wo.getStatus() == WorkOrderStatus.CLOSED || wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot assign a closed or cancelled work order");
        }
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Technician not found"));
        if (technician.getRole() != Role.TECHNICIAN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not a technician");
        }

        WorkOrderStatus from = wo.getStatus();
        wo.setAssignedTo(technician);
        if (from == WorkOrderStatus.NEW) {
            wo.setStatus(WorkOrderStatus.ASSIGNED);
        }
        wo.setUpdatedAt(Instant.now());
        WorkOrder saved = workOrderRepository.save(wo);

        if (from != saved.getStatus()) {
            writeHistory(saved, from, saved.getStatus(), "Assigned to " + technician.getName());
        }
        return saved;
    }

    @Transactional
    public WorkOrder changeStatus(Long workOrderId, WorkOrderStatus toStatus, String note) {
        WorkOrder wo = get(workOrderId);
        WorkOrderStatus from = wo.getStatus();

        Set<WorkOrderStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(toStatus)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Illegal transition: " + from + " -> " + toStatus);
        }

        Set<Role> allowedRoles = TRANSITION_ROLES.get(toStatus);
        if (allowedRoles != null && !allowedRoles.isEmpty() && !currentUserHasAnyRole(allowedRoles)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your role cannot perform this transition");
        }

        if ((toStatus == WorkOrderStatus.IN_PROGRESS || toStatus == WorkOrderStatus.ON_HOLD
                || toStatus == WorkOrderStatus.COMPLETED) && !currentUserHasAnyRole(Set.of(Role.MANAGER))) {
            User current = currentUser();
            if (wo.getAssignedTo() == null || !wo.getAssignedTo().getId().equals(current.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Only the assigned technician can update this job");
            }
        }

        wo.setStatus(toStatus);
        wo.setUpdatedAt(Instant.now());
        WorkOrder saved = workOrderRepository.save(wo);
        writeHistory(saved, from, toStatus, note);
        return saved;
    }

    @Transactional
    public void logPartUsage(Long workOrderId, Long partId, int qty) {
        WorkOrder wo = get(workOrderId);
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Part not found"));

        if (part.getStockQty() < qty) {
            throw new ApiException(HttpStatus.CONFLICT, "Insufficient stock for part " + part.getSku());
        }

        part.setStockQty(part.getStockQty() - qty);
        partRepository.save(part);

        PartUsage usage = new PartUsage();
        usage.setWorkOrder(wo);
        usage.setPart(part);
        usage.setQtyUsed(qty);
        partUsageRepository.save(usage);
    }

    @Transactional
    public void logTime(Long workOrderId, int minutes, String note) {
        WorkOrder wo = get(workOrderId);
        User technician = currentUser();

        TimeLog log = new TimeLog();
        log.setWorkOrder(wo);
        log.setTechnician(technician);
        log.setMinutes(minutes);
        log.setNote(note);
        timeLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto summary() {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        long total = 0;
        for (WorkOrderStatus s : WorkOrderStatus.values()) {
            long c = workOrderRepository.countByStatus(s);
            counts.put(s.name(), c);
            total += c;
        }

        List<WorkOrder> all = workOrderRepository.findAll();
        long overdue = all.stream()
                .filter(w -> w.getSlaDueAt() != null && w.getSlaDueAt().isBefore(Instant.now())
                        && w.getStatus() != WorkOrderStatus.CLOSED && w.getStatus() != WorkOrderStatus.CANCELLED)
                .count();

        long closedCount = counts.getOrDefault("CLOSED", 0L);
        long closedOnTime = all.stream()
                .filter(w -> w.getStatus() == WorkOrderStatus.CLOSED)
                .filter(w -> w.getSlaDueAt() == null || w.getUpdatedAt().isBefore(w.getSlaDueAt()))
                .count();
        double slaCompliance = closedCount == 0 ? 100.0 : (closedOnTime * 100.0 / closedCount);

        Map<String, Long> byTechnician = all.stream()
                .filter(w -> w.getStatus() != WorkOrderStatus.CLOSED && w.getStatus() != WorkOrderStatus.CANCELLED)
                .filter(w -> w.getAssignedTo() != null)
                .collect(Collectors.groupingBy(w -> w.getAssignedTo().getName(), Collectors.counting()));

        Map<String, Long> bySite = all.stream()
                .filter(w -> w.getStatus() != WorkOrderStatus.CLOSED && w.getStatus() != WorkOrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(w -> w.getSite().getName(), Collectors.counting()));

        return new DashboardSummaryDto(counts, overdue, Math.round(slaCompliance * 10.0) / 10.0, byTechnician, bySite);
    }

    private void writeHistory(WorkOrder wo, WorkOrderStatus from, WorkOrderStatus to, String note) {
        WorkOrderStatusHistory h = new WorkOrderStatusHistory();
        h.setWorkOrder(wo);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(safeCurrentUser());
        h.setNote(note);
        historyRepository.save(h);
    }

    private String generateCode() {
        long seq = workOrderRepository.count() + 1;
        return String.format("WO-%d-%04d", java.time.Year.now().getValue(), seq);
    }

    private User currentUser() {
        User u = safeCurrentUser();
        if (u == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }
        return u;
    }

    private User safeCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private boolean currentUserHasAnyRole(Set<Role> roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Set<String> names = roles.stream().map(r -> "ROLE_" + r.name()).collect(Collectors.toSet());
        return auth.getAuthorities().stream().anyMatch(a -> names.contains(a.getAuthority()));
    }
}

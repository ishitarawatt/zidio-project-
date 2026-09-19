package com.zidio.keystone.service;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.exception.ApiException;
import com.zidio.keystone.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers the two things the brief explicitly calls out as highest-value to test
 * (Section 16.1): the guarded work-order lifecycle, and the authorisation rules
 * that sit on top of it. Every test that needs an authenticated caller sets up
 * SecurityContextHolder directly rather than standing up a full Spring context --
 * that keeps these fast and focused on WorkOrderService's own logic.
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private WorkOrderStatusHistoryRepository historyRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private UserRepository userRepository;
    @Mock private PartRepository partRepository;
    @Mock private PartUsageRepository partUsageRepository;
    @Mock private TimeLogRepository timeLogRepository;

    @InjectMocks
    private WorkOrderService workOrderService;

    private Customer customer;
    private Site site;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setName("Meridian Facilities Management");

        site = new Site();
        site.setId(10L);
        site.setCustomer(customer);
        site.setName("Meridian HQ Tower");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private WorkOrder workOrder(WorkOrderStatus status, User assignedTo) {
        WorkOrder wo = new WorkOrder();
        wo.setId(100L);
        wo.setCode("WO-2026-0001");
        wo.setTitle("AC not cooling");
        wo.setPriority(Priority.HIGH);
        wo.setStatus(status);
        wo.setCustomer(customer);
        wo.setSite(site);
        wo.setAssignedTo(assignedTo);
        return wo;
    }

    private User user(long id, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setName(email);
        u.setRole(role);
        return u;
    }

    private void authenticateAs(User u) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
        var auth = new UsernamePasswordAuthenticationToken(u.getEmail(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    // ---------- lifecycle: illegal transitions ----------

    @Test
    void rejectsIllegalTransition_newDirectlyToCompleted() {
        WorkOrder wo = workOrder(WorkOrderStatus.NEW, null);
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));

        User manager = user(1L, "manager@keystone.demo", Role.MANAGER);
        authenticateAs(manager);

        ApiException ex = assertThrows(ApiException.class,
                () -> workOrderService.changeStatus(100L, WorkOrderStatus.COMPLETED, null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void rejectsIllegalTransition_fromTerminalClosedState() {
        WorkOrder wo = workOrder(WorkOrderStatus.CLOSED, null);
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));

        User manager = user(1L, "manager@keystone.demo", Role.MANAGER);
        authenticateAs(manager);

        ApiException ex = assertThrows(ApiException.class,
                () -> workOrderService.changeStatus(100L, WorkOrderStatus.IN_PROGRESS, null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ---------- lifecycle: legal transitions succeed and write history ----------

    @Test
    void allowsLegalTransition_assignedToInProgress_byAssignedTechnician() {
        User technician = user(3L, "technician@keystone.demo", Role.TECHNICIAN);
        WorkOrder wo = workOrder(WorkOrderStatus.ASSIGNED, technician);

        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticateAs(technician);

        WorkOrder result = workOrderService.changeStatus(100L, WorkOrderStatus.IN_PROGRESS, "Starting work");

        assertEquals(WorkOrderStatus.IN_PROGRESS, result.getStatus());
        verify(historyRepository).save(argThat(h ->
                h.getFromStatus() == WorkOrderStatus.ASSIGNED && h.getToStatus() == WorkOrderStatus.IN_PROGRESS));
    }

    // ---------- authorization: role-gated transitions ----------

    @Test
    void rejectsClose_whenCallerIsNotManager() {
        User dispatcher = user(2L, "dispatcher@keystone.demo", Role.DISPATCHER);
        WorkOrder wo = workOrder(WorkOrderStatus.COMPLETED, null);
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));

        authenticateAs(dispatcher);

        ApiException ex = assertThrows(ApiException.class,
                () -> workOrderService.changeStatus(100L, WorkOrderStatus.CLOSED, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void allowsClose_whenCallerIsManager() {
        User manager = user(1L, "manager@keystone.demo", Role.MANAGER);
        WorkOrder wo = workOrder(WorkOrderStatus.COMPLETED, null);
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticateAs(manager);

        WorkOrder result = workOrderService.changeStatus(100L, WorkOrderStatus.CLOSED, "Signed off");

        assertEquals(WorkOrderStatus.CLOSED, result.getStatus());
    }

    @Test
    void rejectsInProgress_whenTechnicianIsNotTheAssignee() {
        User assignedTech = user(3L, "technician@keystone.demo", Role.TECHNICIAN);
        User otherTech = user(4L, "other-technician@keystone.demo", Role.TECHNICIAN);
        WorkOrder wo = workOrder(WorkOrderStatus.ASSIGNED, assignedTech);

        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));

        authenticateAs(otherTech);

        ApiException ex = assertThrows(ApiException.class,
                () -> workOrderService.changeStatus(100L, WorkOrderStatus.IN_PROGRESS, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(workOrderRepository, never()).save(any());
    }

    // ---------- assignment ----------

    @Test
    void assign_movesNewWorkOrderToAssigned() {
        User technician = user(3L, "technician@keystone.demo", Role.TECHNICIAN);
        WorkOrder wo = workOrder(WorkOrderStatus.NEW, null);

        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(userRepository.findById(3L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkOrder result = workOrderService.assign(100L, 3L);

        assertEquals(WorkOrderStatus.ASSIGNED, result.getStatus());
        assertEquals(3L, result.getAssignedTo().getId());
    }

    @Test
    void assign_rejectsNonTechnicianUser() {
        User dispatcher = user(2L, "dispatcher@keystone.demo", Role.DISPATCHER);
        WorkOrder wo = workOrder(WorkOrderStatus.NEW, null);

        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(userRepository.findById(2L)).thenReturn(Optional.of(dispatcher));

        ApiException ex = assertThrows(ApiException.class, () -> workOrderService.assign(100L, 2L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void assign_rejectsWhenWorkOrderIsClosed() {
        WorkOrder wo = workOrder(WorkOrderStatus.CLOSED, null);
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));

        ApiException ex = assertThrows(ApiException.class, () -> workOrderService.assign(100L, 3L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ---------- transactional integrity: parts/stock ----------

    @Test
    void logPartUsage_decrementsStockInSameOperation() {
        WorkOrder wo = workOrder(WorkOrderStatus.IN_PROGRESS, null);
        Part part = new Part();
        part.setId(5L);
        part.setSku("HVAC-FLT-2020");
        part.setName("HVAC Filter");
        part.setUnitCost(BigDecimal.valueOf(12.50));
        part.setStockQty(10);

        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(partRepository.findById(5L)).thenReturn(Optional.of(part));

        workOrderService.logPartUsage(100L, 5L, 3);

        assertEquals(7, part.getStockQty());
        verify(partRepository).save(part);
        verify(partUsageRepository).save(argThat(u -> u.getQtyUsed() == 3));
    }

    @Test
    void logPartUsage_rejectsWhenStockInsufficient() {
        WorkOrder wo = workOrder(WorkOrderStatus.IN_PROGRESS, null);
        Part part = new Part();
        part.setId(5L);
        part.setSku("HVAC-FLT-2020");
        part.setStockQty(2);

        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(partRepository.findById(5L)).thenReturn(Optional.of(part));

        ApiException ex = assertThrows(ApiException.class,
                () -> workOrderService.logPartUsage(100L, 5L, 5));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(2, part.getStockQty(), "Stock must not change when the operation is rejected");
        verify(partUsageRepository, never()).save(any());
    }

    // ---------- data integrity on create ----------

    @Test
    void create_rejectsSiteThatDoesNotBelongToTheGivenCustomer() {
        Customer otherCustomer = new Customer();
        otherCustomer.setId(99L);
        otherCustomer.setName("Harborview Offices Ltd");

        var req = new com.zidio.keystone.dto.CreateWorkOrderRequest();
        req.setTitle("Broken light");
        req.setPriority(Priority.LOW);
        req.setCustomerId(99L);
        req.setSiteId(10L); // belongs to `customer` (id=1), not `otherCustomer` (id=99)

        when(customerRepository.findById(99L)).thenReturn(Optional.of(otherCustomer));
        when(siteRepository.findById(10L)).thenReturn(Optional.of(site));

        ApiException ex = assertThrows(ApiException.class, () -> workOrderService.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}

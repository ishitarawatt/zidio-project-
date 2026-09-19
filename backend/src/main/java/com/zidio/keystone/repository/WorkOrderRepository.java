package com.zidio.keystone.repository;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Page<WorkOrder> findByCustomerId(Long customerId, Pageable pageable);
    Page<WorkOrder> findByAssignedToId(Long userId, Pageable pageable);
    Page<WorkOrder> findByStatus(WorkOrderStatus status, Pageable pageable);
    long countByStatus(WorkOrderStatus status);
}

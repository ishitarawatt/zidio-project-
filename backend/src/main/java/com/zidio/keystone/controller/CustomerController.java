package com.zidio.keystone.controller;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.dto.CustomerDto;
import com.zidio.keystone.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public Page<Customer> list(@RequestParam(required = false) String search, Pageable pageable) {
        return customerService.list(search, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public Customer get(@PathVariable Long id) {
        return customerService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<Customer> create(@Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.ok(customerService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public Customer update(@PathVariable Long id, @Valid @RequestBody CustomerDto dto) {
        return customerService.update(id, dto);
    }
}

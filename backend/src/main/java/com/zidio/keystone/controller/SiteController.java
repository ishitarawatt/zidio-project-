package com.zidio.keystone.controller;

import com.zidio.keystone.domain.Site;
import com.zidio.keystone.dto.SiteDto;
import com.zidio.keystone.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping("/customers/{customerId}/sites")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public List<Site> listForCustomer(@PathVariable Long customerId) {
        return siteService.listByCustomer(customerId);
    }

    @PostMapping("/sites")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<Site> create(@Valid @RequestBody SiteDto dto) {
        return ResponseEntity.ok(siteService.create(dto));
    }
}

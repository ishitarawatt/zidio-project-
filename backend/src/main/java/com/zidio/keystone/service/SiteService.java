package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.domain.Site;
import com.zidio.keystone.dto.SiteDto;
import com.zidio.keystone.exception.ApiException;
import com.zidio.keystone.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerService customerService;

    public List<Site> listByCustomer(Long customerId) {
        return siteRepository.findByCustomerId(customerId);
    }

    public Site get(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Site not found"));
    }

    @Transactional
    public Site create(SiteDto dto) {
        Customer customer = customerService.get(dto.getCustomerId());
        Site site = new Site();
        site.setCustomer(customer);
        site.setName(dto.getName());
        site.setAddress(dto.getAddress());
        return siteRepository.save(site);
    }
}

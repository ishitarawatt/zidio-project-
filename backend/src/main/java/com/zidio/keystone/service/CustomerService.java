package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.dto.CustomerDto;
import com.zidio.keystone.exception.ApiException;
import com.zidio.keystone.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Page<Customer> list(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return customerRepository.findAll(pageable);
        }
        return customerRepository.findByNameContainingIgnoreCase(search, pageable);
    }

    public Customer get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    @Transactional
    public Customer create(CustomerDto dto) {
        Customer c = new Customer();
        c.setName(dto.getName());
        c.setContactEmail(dto.getContactEmail());
        return customerRepository.save(c);
    }

    @Transactional
    public Customer update(Long id, CustomerDto dto) {
        Customer c = get(id);
        c.setName(dto.getName());
        c.setContactEmail(dto.getContactEmail());
        return customerRepository.save(c);
    }
}

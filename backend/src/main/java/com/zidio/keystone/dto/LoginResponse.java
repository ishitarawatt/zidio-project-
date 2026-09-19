package com.zidio.keystone.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String role;
    private String name;
    private Long customerId; // set only for CUSTOMER-role users; null otherwise
}

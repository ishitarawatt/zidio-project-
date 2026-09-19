package com.zidio.keystone.controller;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Lists users, optionally filtered by role. Used by dispatchers/managers to
     * populate the technician-assignment picker. Deliberately returns only the
     * fields the UI needs -- never the password hash.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public List<Map<String, Object>> list(@RequestParam(required = false) Role role) {
        List<User> users = (role != null)
                ? userRepository.findAll().stream().filter(u -> u.getRole() == role).toList()
                : userRepository.findAll();

        return users.stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "role", u.getRole().name()
                ))
                .toList();
    }
}

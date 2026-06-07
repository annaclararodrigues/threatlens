package com.backend.threatlens.controller;

import com.backend.threatlens.dto.response.PageResponseDTO;
import com.backend.threatlens.dto.response.UserSummaryResponseDTO;
import com.backend.threatlens.enums.Role;
import com.backend.threatlens.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<PageResponseDTO<UserSummaryResponseDTO>> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.listUsers(role, search, pageable));
    }
}

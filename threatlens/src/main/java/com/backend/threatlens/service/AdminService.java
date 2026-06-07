package com.backend.threatlens.service;

import com.backend.threatlens.dto.response.PageResponseDTO;
import com.backend.threatlens.dto.response.UserSummaryResponseDTO;
import com.backend.threatlens.enums.Role;
import com.backend.threatlens.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponseDTO<UserSummaryResponseDTO> listUsers(Role role, String search, Pageable pageable) {
        String normalizedSearch = null;
        if (search != null && !search.isBlank()) {
            String escaped = search.toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            normalizedSearch = "%" + escaped + "%";
        }
        return PageResponseDTO.from(
                userRepository
                        .findAllWithFilters(role, normalizedSearch, pageable)
                        .map(u -> new UserSummaryResponseDTO(
                                u.getId(),
                                u.getUsername(),
                                u.getEmail(),
                                u.getRole(),
                                u.isEmailVerified()
                        ))
        );
    }
}

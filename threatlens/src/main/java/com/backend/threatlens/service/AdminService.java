package com.backend.threatlens.service;

import com.backend.threatlens.dto.response.PageResponseDTO;
import com.backend.threatlens.dto.response.UserSummaryResponseDTO;
import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.enums.Role;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.exception.ResourceNotFoundException;
import com.backend.threatlens.repository.RefreshTokenRepository;
import com.backend.threatlens.repository.UserRepository;
import com.backend.threatlens.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeRepository verificationCodeRepository;

    @Transactional
    public void deleteUser(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessRuleViolationException("Não é permitido remover um administrador.");
        }
        refreshTokenRepository.deleteByEmail(user.getEmail());
        verificationCodeRepository.deleteByEmail(user.getEmail());
        userRepository.delete(user);
    }

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

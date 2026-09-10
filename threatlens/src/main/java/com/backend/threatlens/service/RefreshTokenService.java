package com.backend.threatlens.service;

import com.backend.threatlens.entity.RefreshTokenEntity;
import com.backend.threatlens.exception.InvalidTokenException;
import com.backend.threatlens.repository.RefreshTokenRepository;
import com.backend.threatlens.utils.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public Optional<String> findEmailByToken(String token) {
        return refreshTokenRepository.findByToken(token).map(RefreshTokenEntity::getEmail);
    }

    @Transactional
    public String createRefreshToken(String email) {
        refreshTokenRepository.deleteByEmail(email);

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .token(jwtUtil.generateRefreshToken(email))
                .email(email)
                .expiresAt((LocalDateTime.now().plusDays(7))) // 7 days
                .build();
        refreshTokenRepository.save(entity);

        return entity.getToken();
    }

    public RefreshTokenEntity validate(String token) {
        if (!jwtUtil.isValidToken(token)) {
            throw new InvalidTokenException("Refresh token inválido ou expirado.");
        }

        RefreshTokenEntity entity = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token revogado ou não encontrado."));

        return entity;
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByTokenAndIsRevokedFalse(token)
                .ifPresent(entity -> {
                    entity.setRevoked(true);
                    refreshTokenRepository.save(entity);
                });
    }

}

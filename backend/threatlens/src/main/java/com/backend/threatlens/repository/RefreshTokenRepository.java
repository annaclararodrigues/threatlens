package com.backend.threatlens.repository;

import com.backend.threatlens.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenAndIsRevokedFalse(String token);

    Optional<RefreshTokenEntity> findByToken(String token);

    void deleteByEmail(String email);
}
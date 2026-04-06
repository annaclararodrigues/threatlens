package com.backend.threatlens.repository;

import com.backend.threatlens.entity.VerificationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, UUID> {
    Optional<VerificationCodeEntity> findByEmailAndCodeAndIsUsedFalse(String email, String code);

    void deleteByEmail(String email);
}

package com.backend.threatlens.repository;

import com.backend.threatlens.entity.VerificationCodeEntity;
import com.backend.threatlens.enums.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, UUID> {

    Optional<VerificationCodeEntity> findByEmailAndCodeAndCodeTypeAndIsUsedFalse(String email, String code, CodeType codeType);

    void deleteByEmail(String email);

    void deleteByEmailAndCodeType(String email, CodeType codeType);
}

package com.backend.threatlens.entity;

import com.backend.threatlens.enums.CodeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_code")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    @Enumerated(EnumType.STRING)
    private CodeType codeType;

    private String code;

    private LocalDateTime expiresAt;

    private boolean isUsed = false;
}

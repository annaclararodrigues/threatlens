package com.backend.threatlens.service;

import com.backend.threatlens.dto.request.*;
import com.backend.threatlens.dto.response.AuthTokens;
import com.backend.threatlens.dto.response.MessageResponseDTO;
import com.backend.threatlens.entity.RefreshTokenEntity;
import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.entity.VerificationCodeEntity;
import com.backend.threatlens.enums.CodeType;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.exception.EmailNotVerifiedException;
import com.backend.threatlens.exception.InvalidRequestException;
import com.backend.threatlens.exception.ResourceNotFoundException;
import com.backend.threatlens.repository.UserRepository;
import com.backend.threatlens.repository.VerificationCodeRepository;
import com.backend.threatlens.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public MessageResponseDTO register(RegisterRequestDTO registerRequestDTO) {
        if (userRepository.findByEmail(registerRequestDTO.email()).isPresent()) {
            throw new BusinessRuleViolationException("Email already in use");
        }

        UserEntity user = UserEntity.builder()
                .username(registerRequestDTO.username())
                .email(registerRequestDTO.email())
                .password(passwordEncoder.encode(registerRequestDTO.password()))
                .build();
        userRepository.save(user);

        sendNewVerificationCode(registerRequestDTO.email());

        audit("REGISTER", registerRequestDTO.email());

        return new MessageResponseDTO("Código de verificação enviado para " + registerRequestDTO.email());
    }

    @Transactional
    public AuthTokens verifyCode(VerifyCodeRequestDTO dto) {
        VerificationCodeEntity code = verificationCodeRepository
                .findByEmailAndCodeAndCodeTypeAndIsUsedFalse(dto.email(), dto.code(), dto.codeType())
                .orElseThrow(() -> new BusinessRuleViolationException("Código inválido ou já utilizado."));

        if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleViolationException("Código expirado. Solicite um novo.");
        }

        code.setUsed(true);
        verificationCodeRepository.save(code);

        UserEntity user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (dto.codeType() == CodeType.REGISTER) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return new AuthTokens(accessToken, refreshToken, user.getUsername(), user.getEmail(), user.getRole());
    }

    public AuthTokens login(LoginRequestDTO loginRequestDTO) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequestDTO.email(),
                    loginRequestDTO.password()
            ));
        } catch (BadCredentialsException ex) {
            audit("LOGIN_FAILED", loginRequestDTO.email());
            throw ex;
        }

        UserEntity user = userRepository.findByEmail(loginRequestDTO.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + loginRequestDTO.email()));

        if (!user.isEmailVerified()) {
            audit("LOGIN_FAILED", loginRequestDTO.email());
            throw new EmailNotVerifiedException("E-mail não verificado. Verifique sua caixa de entrada.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        audit("LOGIN_SUCCESS", user.getEmail());

        return new AuthTokens(accessToken, refreshToken, user.getUsername(), user.getEmail(), user.getRole());
    }

    @Transactional
    public MessageResponseDTO resendCode(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (user.isEmailVerified()) {
            throw new BusinessRuleViolationException("E-mail já verificado.");
        }

        sendNewVerificationCode(email);

        return new MessageResponseDTO("Novo código enviado para " + email);
    }

    private void sendNewVerificationCode(String email) {
        String code = String.format("%04d", new Random().nextInt(10000));

        verificationCodeRepository.deleteByEmailAndCodeType(email, CodeType.REGISTER);

        VerificationCodeEntity verificationCode = VerificationCodeEntity.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .codeType(CodeType.REGISTER)
                .build();
        verificationCodeRepository.save(verificationCode);

        emailService.sendVerificationCode(email, code);
    }

    @Transactional
    public MessageResponseDTO resendPasswordCode(String email) {
        userRepository.findByEmail(email).ifPresent(user -> issueResetPasswordCode(email));

        return new MessageResponseDTO("Se este e-mail estiver cadastrado, um novo código foi enviado.");
    }

    @Transactional
    public MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO dto) {
        userRepository.findByEmail(dto.email()).ifPresent(user -> issueResetPasswordCode(dto.email()));

        return new MessageResponseDTO("Se este e-mail estiver cadastrado, você receberá um código de redefinição.");
    }

    private void issueResetPasswordCode(String email) {
        verificationCodeRepository.deleteByEmailAndCodeType(email, CodeType.RESET_PASSWORD);

        String code = String.format("%04d", new Random().nextInt(10000));

        VerificationCodeEntity entity = VerificationCodeEntity.builder()
                .email(email)
                .codeType(CodeType.RESET_PASSWORD)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        verificationCodeRepository.save(entity);
        emailService.sendVerificationCode(email, code);
    }

    @Transactional
    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO dto, String email) {
        if (!dto.password().equals(dto.passwordConfirm())) {
            throw new InvalidRequestException("As senhas não coincidem.");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        user.setPassword(passwordEncoder.encode(dto.password()));
        userRepository.save(user);

        audit("PASSWORD_CHANGE", email);

        return new MessageResponseDTO("Senha redefinida com sucesso.");
    }

    public MessageResponseDTO changePassword(ChangePasswordRequestDTO dto, String email) {
        if (!dto.newPassword().equals(dto.newPasswordConfirm())) {
            throw new InvalidRequestException("As senhas não coincidem.");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new BusinessRuleViolationException("Senha atual incorreta.");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        audit("PASSWORD_CHANGE", email);

        return new MessageResponseDTO("Senha alterada com sucesso.");
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
            audit("LOGOUT", null);
        }
    }

    public AuthTokens refresh(String refreshToken) {
        RefreshTokenEntity entity = refreshTokenService.validate(refreshToken);

        refreshTokenService.revoke(refreshToken);

        UserEntity user = userRepository.findByEmail(entity.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String newRefreshToken = refreshTokenService.createRefreshToken(entity.getEmail());

        audit("TOKEN_REFRESH", user.getEmail());

        return new AuthTokens(accessToken, newRefreshToken, user.getUsername(), user.getEmail(), user.getRole());
    }

    private void audit(String event, String email) {
        AUDIT_LOG.info("event={} email={} ip={} timestamp={}", event, email, currentClientIp(), LocalDateTime.now());
    }

    private String currentClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest().getRemoteAddr();
        } catch (IllegalStateException ex) {
            return "unknown";
        }
    }

}
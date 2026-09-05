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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationCodeRepository verificationCodeRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    // --- register ---

    @Test
    void register_success() {
        RegisterRequestDTO dto = new RegisterRequestDTO("user", "user@test.com", "Password1!");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendVerificationCode(any(), any());

        MessageResponseDTO result = authService.register(dto);

        assertThat(result.message()).contains("user@test.com");
        verify(userRepository).save(any(UserEntity.class));
        verify(emailService).sendVerificationCode(eq("user@test.com"), any());
    }

    @Test
    void register_emailAlreadyInUse_throwsException() {
        RegisterRequestDTO dto = new RegisterRequestDTO("user", "user@test.com", "Password1!");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(buildUser(true)));

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Email already in use");
    }

    // --- login ---

    @Test
    void login_success() {
        LoginRequestDTO dto = new LoginRequestDTO("user@test.com", "Password1!");
        UserEntity user = buildUser(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("user@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken("user@test.com")).thenReturn("refresh-token");

        AuthTokens tokens = authService.login(dto);

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
        assertThat(tokens.email()).isEqualTo("user@test.com");
    }

    @Test
    void login_emailNotVerified_throwsException() {
        LoginRequestDTO dto = new LoginRequestDTO("user@test.com", "Password1!");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(buildUser(false)));

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("E-mail não verificado");
    }

    // --- verifyCode ---

    @Test
    void verifyCode_registerType_success() {
        VerifyCodeRequestDTO dto = new VerifyCodeRequestDTO("user@test.com", CodeType.REGISTER, "1234");
        VerificationCodeEntity code = buildCode(LocalDateTime.now().plusMinutes(5));
        UserEntity user = buildUser(false);

        when(verificationCodeRepository.findByEmailAndCodeAndCodeTypeAndIsUsedFalse(
                "user@test.com", "1234", CodeType.REGISTER)).thenReturn(Optional.of(code));
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateAccessToken("user@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken("user@test.com")).thenReturn("refresh-token");

        AuthTokens tokens = authService.verifyCode(dto);

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void verifyCode_invalidCode_throwsException() {
        VerifyCodeRequestDTO dto = new VerifyCodeRequestDTO("user@test.com", CodeType.REGISTER, "0000");
        when(verificationCodeRepository.findByEmailAndCodeAndCodeTypeAndIsUsedFalse(
                "user@test.com", "0000", CodeType.REGISTER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyCode(dto))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Código inválido");
    }

    @Test
    void verifyCode_expiredCode_throwsException() {
        VerifyCodeRequestDTO dto = new VerifyCodeRequestDTO("user@test.com", CodeType.REGISTER, "1234");
        VerificationCodeEntity code = buildCode(LocalDateTime.now().minusMinutes(1));
        when(verificationCodeRepository.findByEmailAndCodeAndCodeTypeAndIsUsedFalse(
                "user@test.com", "1234", CodeType.REGISTER)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> authService.verifyCode(dto))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Código expirado");
    }

    // --- resendCode ---

    @Test
    void resendCode_success() {
        UserEntity user = buildUser(false);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(verificationCodeRepository).deleteByEmailAndCodeType("user@test.com", CodeType.REGISTER);
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendVerificationCode(any(), any());

        MessageResponseDTO result = authService.resendCode("user@test.com");

        assertThat(result.message()).contains("user@test.com");
        verify(verificationCodeRepository).save(any(VerificationCodeEntity.class));
        verify(emailService).sendVerificationCode(eq("user@test.com"), any());
    }

    @Test
    void resendCode_userNotFound_throwsException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resendCode("user@test.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    void resendCode_emailAlreadyVerified_throwsException() {
        UserEntity user = buildUser(true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resendCode("user@test.com"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("E-mail já verificado");
    }

    // --- forgotPassword ---

    @Test
    void forgotPassword_success() {
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO("user@test.com");
        UserEntity user = buildUser(true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(verificationCodeRepository).deleteByEmailAndCodeType("user@test.com", CodeType.RESET_PASSWORD);
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendVerificationCode(any(), any());

        MessageResponseDTO result = authService.forgotPassword(dto);

        assertThat(result.message()).contains("redefinição");
        verify(verificationCodeRepository).save(any(VerificationCodeEntity.class));
        verify(emailService).sendVerificationCode(eq("user@test.com"), any());
    }

    @Test
    void forgotPassword_userNotFound_returnsSameGenericMessageWithoutSendingEmail() {
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        MessageResponseDTO result = authService.forgotPassword(dto);

        assertThat(result.message()).contains("Se este e-mail estiver cadastrado");
        verifyNoInteractions(emailService);
        verify(verificationCodeRepository, org.mockito.Mockito.never()).save(any());
    }

    // --- resendPasswordCode ---

    @Test
    void resendPasswordCode_success() {
        UserEntity user = buildUser(true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(verificationCodeRepository).deleteByEmailAndCodeType("user@test.com", CodeType.RESET_PASSWORD);
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendVerificationCode(any(), any());

        MessageResponseDTO result = authService.resendPasswordCode("user@test.com");

        assertThat(result.message()).contains("novo código foi enviado");
        verify(verificationCodeRepository).save(any(VerificationCodeEntity.class));
        verify(emailService).sendVerificationCode(eq("user@test.com"), any());
    }

    @Test
    void resendPasswordCode_userNotFound_returnsSameGenericMessageWithoutSendingEmail() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        MessageResponseDTO result = authService.resendPasswordCode("user@test.com");

        assertThat(result.message()).contains("Se este e-mail estiver cadastrado");
        verifyNoInteractions(emailService);
        verify(verificationCodeRepository, org.mockito.Mockito.never()).save(any());
    }

    // --- changePassword ---

    @Test
    void changePassword_success() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("OldPass1!", "NewPass1!", "NewPass1!");
        UserEntity user = buildUser(true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1!", "hashed-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded-new");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MessageResponseDTO result = authService.changePassword(dto, "user@test.com");

        assertThat(result.message()).contains("Senha alterada");
        assertThat(user.getPassword()).isEqualTo("encoded-new");
    }

    @Test
    void changePassword_passwordsMismatch_throwsException() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("OldPass1!", "NewPass1!", "Different1!");

        assertThatThrownBy(() -> authService.changePassword(dto, "user@test.com"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("senhas não coincidem");
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsException() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("WrongPass!", "NewPass1!", "NewPass1!");
        UserEntity user = buildUser(true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass!", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(dto, "user@test.com"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Senha atual incorreta");
    }

    // --- resetPassword ---

    @Test
    void resetPassword_success() {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO("NewPass1!", "NewPass1!");
        UserEntity user = buildUser(true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded-new");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MessageResponseDTO result = authService.resetPassword(dto, "user@test.com");

        assertThat(result.message()).contains("Senha redefinida");
        assertThat(user.getPassword()).isEqualTo("encoded-new");
    }

    @Test
    void resetPassword_passwordsMismatch_throwsException() {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO("NewPass1!", "Different1!");

        assertThatThrownBy(() -> authService.resetPassword(dto, "user@test.com"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("senhas não coincidem");
    }

    // --- logout ---

    @Test
    void logout_validToken_revokes() {
        doNothing().when(refreshTokenService).revoke("refresh-token");

        authService.logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
    }

    @Test
    void logout_nullToken_doesNotRevoke() {
        authService.logout(null);

        verifyNoInteractions(refreshTokenService);
    }

    // --- refresh ---

    @Test
    void refresh_success() {
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .token("refresh-token")
                .email("user@test.com")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        UserEntity user = buildUser(true);

        when(refreshTokenService.validate("refresh-token")).thenReturn(entity);
        doNothing().when(refreshTokenService).revoke("refresh-token");
        when(jwtUtil.generateAccessToken("user@test.com")).thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken("user@test.com")).thenReturn("new-refresh-token");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        AuthTokens tokens = authService.refresh("refresh-token");

        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).revoke("refresh-token");
    }

    // --- helpers ---

    private UserEntity buildUser(boolean emailVerified) {
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("user@test.com")
                .password("hashed-password")
                .build();
        user.setEmailVerified(emailVerified);
        return user;
    }

    private VerificationCodeEntity buildCode(LocalDateTime expiresAt) {
        return VerificationCodeEntity.builder()
                .email("user@test.com")
                .code("1234")
                .codeType(CodeType.REGISTER)
                .expiresAt(expiresAt)
                .build();
    }
}

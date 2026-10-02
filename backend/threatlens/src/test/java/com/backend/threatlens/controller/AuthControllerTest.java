package com.backend.threatlens.controller;

import com.backend.threatlens.dto.response.AuthTokens;
import com.backend.threatlens.dto.response.MessageResponseDTO;
import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.enums.Role;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.exception.GlobalExceptionHandler;
import com.backend.threatlens.exception.InvalidRequestException;
import com.backend.threatlens.security.UserPrincipal;
import com.backend.threatlens.service.AuthService;
import com.backend.threatlens.utils.CookieUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CookieUtil cookieUtil = new CookieUtil();
        ReflectionTestUtils.setField(cookieUtil, "secureCookie", false);
        ReflectionTestUtils.setField(cookieUtil, "accessTokenExpirationMs", 300_000L);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, cookieUtil))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        UserEntity user = UserEntity.builder().email(email).password("hash").role(Role.USER).build();
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(new UserPrincipal(user), null));
    }

    @Nested
    class Register {

        @Test
        void validRequest_returns201() throws Exception {
            when(authService.register(any())).thenReturn(new MessageResponseDTO("Código de verificação enviado para anna@test.com"));

            mockMvc.perform(post("/auth/register")
                            .contentType("application/json")
                            .content("""
                                    {"username":"anna","email":"anna@test.com","password":"Senha123@"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Código de verificação enviado para anna@test.com"));
        }

        @Test
        void blankEmail_returns422() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType("application/json")
                            .content("""
                                    {"username":"anna","email":"","password":"Senha123@"}
                                    """))
                    .andExpect(status().is(422));
        }
    }

    @Nested
    class Login {

        @Test
        void validCredentials_returns200WithCookies() throws Exception {
            when(authService.login(any())).thenReturn(new AuthTokens("access-tok", "refresh-tok", "anna", "anna@test.com", Role.USER));

            mockMvc.perform(post("/auth/login")
                            .contentType("application/json")
                            .content("""
                                    {"email":"anna@test.com","password":"Senha123@"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("anna"))
                    .andExpect(jsonPath("$.email").value("anna@test.com"))
                    .andExpect(cookie().value(CookieUtil.ACCESS_TOKEN_COOKIE, "access-tok"))
                    .andExpect(cookie().value(CookieUtil.REFRESH_TOKEN_COOKIE, "refresh-tok"));
        }

        @Test
        void badCredentials_returns401() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("E-mail ou senha incorretos."));

            mockMvc.perform(post("/auth/login")
                            .contentType("application/json")
                            .content("""
                                    {"email":"anna@test.com","password":"wrong"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos."));
        }
    }

    @Nested
    class Verify {

        @Test
        void invalidCode_returns409() throws Exception {
            when(authService.verifyCode(any())).thenThrow(new BusinessRuleViolationException("Código inválido ou já utilizado."));

            mockMvc.perform(post("/auth/verify")
                            .contentType("application/json")
                            .content("""
                                    {"email":"anna@test.com","codeType":"REGISTER","code":"0000"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Código inválido ou já utilizado."));
        }
    }

    @Nested
    class Refresh {

        @Test
        void noCookiePresent_returns401() throws Exception {
            mockMvc.perform(post("/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Refresh token não encontrado."));
        }
    }

    @Nested
    class Logout {

        @Test
        void clearsCookiesAndReturns200() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout realizado com sucesso."))
                    .andExpect(cookie().maxAge(CookieUtil.ACCESS_TOKEN_COOKIE, 0))
                    .andExpect(cookie().maxAge(CookieUtil.REFRESH_TOKEN_COOKIE, 0));
        }
    }

    @Nested
    class ResendCode {

        @Test
        void validEmail_returns200() throws Exception {
            when(authService.resendCode("anna@test.com"))
                    .thenReturn(new MessageResponseDTO("Novo código enviado para anna@test.com"));

            mockMvc.perform(post("/auth/resend-code")
                            .contentType("application/json")
                            .content("""
                                    {"email":"anna@test.com"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Novo código enviado para anna@test.com"));
        }

        @Test
        void blankEmail_returns422() throws Exception {
            mockMvc.perform(post("/auth/resend-code")
                            .contentType("application/json")
                            .content("""
                                    {"email":""}
                                    """))
                    .andExpect(status().is(422));
        }
    }

    @Nested
    class ResendPasswordCode {

        @Test
        void validEmail_returns200() throws Exception {
            when(authService.resendPasswordCode("anna@test.com"))
                    .thenReturn(new MessageResponseDTO("Se este e-mail estiver cadastrado, um novo código foi enviado."));

            mockMvc.perform(post("/auth/resend-password")
                            .contentType("application/json")
                            .content("""
                                    {"email":"anna@test.com"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Se este e-mail estiver cadastrado, um novo código foi enviado."));
        }
    }

    @Nested
    class ForgotPassword {

        @Test
        void validEmail_returns200() throws Exception {
            when(authService.forgotPassword(any()))
                    .thenReturn(new MessageResponseDTO("Se este e-mail estiver cadastrado, você receberá um código de redefinição."));

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType("application/json")
                            .content("""
                                    {"email":"anna@test.com"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Se este e-mail estiver cadastrado, você receberá um código de redefinição."));
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void validRequest_returns200() throws Exception {
            authenticateAs("anna@test.com");
            when(authService.resetPassword(any(), eq("anna@test.com")))
                    .thenReturn(new MessageResponseDTO("Senha redefinida com sucesso."));

            mockMvc.perform(post("/auth/reset-password")
                            .contentType("application/json")
                            .content("""
                                    {"password":"NovaSenha123@","passwordConfirm":"NovaSenha123@"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Senha redefinida com sucesso."));
        }

        @Test
        void passwordsMismatch_returns400() throws Exception {
            authenticateAs("anna@test.com");
            when(authService.resetPassword(any(), eq("anna@test.com")))
                    .thenThrow(new InvalidRequestException("As senhas não coincidem."));

            mockMvc.perform(post("/auth/reset-password")
                            .contentType("application/json")
                            .content("""
                                    {"password":"NovaSenha123@","passwordConfirm":"Diferente123@"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("As senhas não coincidem."));
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void validRequest_returns200() throws Exception {
            authenticateAs("anna@test.com");
            when(authService.changePassword(any(), eq("anna@test.com")))
                    .thenReturn(new MessageResponseDTO("Senha alterada com sucesso."));

            mockMvc.perform(post("/auth/change-password")
                            .contentType("application/json")
                            .content("""
                                    {"currentPassword":"Senha123@","newPassword":"NovaSenha123@","newPasswordConfirm":"NovaSenha123@"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Senha alterada com sucesso."));
        }

        @Test
        void wrongCurrentPassword_returns409() throws Exception {
            authenticateAs("anna@test.com");
            when(authService.changePassword(any(), eq("anna@test.com")))
                    .thenThrow(new BusinessRuleViolationException("Senha atual incorreta."));

            mockMvc.perform(post("/auth/change-password")
                            .contentType("application/json")
                            .content("""
                                    {"currentPassword":"errada","newPassword":"NovaSenha123@","newPasswordConfirm":"NovaSenha123@"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Senha atual incorreta."));
        }
    }
}

package com.backend.threatlens.service;

import com.backend.threatlens.dto.request.LoginRequestDTO;
import com.backend.threatlens.dto.request.RegisterRequestDTO;
import com.backend.threatlens.dto.request.VerifyCodeRequestDTO;
import com.backend.threatlens.dto.response.AuthResponseDTO;
import com.backend.threatlens.dto.response.MessageResponseDTO;
import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.entity.VerificationCodeEntity;
import com.backend.threatlens.repository.UserRepository;
import com.backend.threatlens.repository.VerificationCodeRepository;
import com.backend.threatlens.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Transactional
    public MessageResponseDTO register(RegisterRequestDTO registerRequestDTO) {
        if (userRepository.findByEmail(registerRequestDTO.email()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        UserEntity user = new UserEntity();
        user.setUsername(registerRequestDTO.username());
        user.setEmail(registerRequestDTO.email());
        user.setPassword(passwordEncoder.encode(registerRequestDTO.password()));
        userRepository.save(user);

        sendNewVerificationCode(registerRequestDTO.email());

        return new MessageResponseDTO("Código de verificação enviado para " + registerRequestDTO.email());
    }

    @Transactional
    public AuthResponseDTO verifyCode(VerifyCodeRequestDTO dto) {
        VerificationCodeEntity verificationCode = verificationCodeRepository
                .findByEmailAndCodeAndIsUsedFalse(dto.email(), dto.code())
                .orElseThrow(() -> new RuntimeException("Código inválido ou já utilizado."));

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Código expirado. Solicite um novo.");
        }

        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        UserEntity user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        user.setEmailVerified(true);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponseDTO(token, user.getUsername(), user.getEmail());
    }

    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequestDTO.email(),
                loginRequestDTO.password()
        ));

        UserEntity user = userRepository.findByEmail(loginRequestDTO.email())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + loginRequestDTO.email()));

        if (!user.isEmailVerified()) {
            throw new RuntimeException("E-mail não verificado. Verifique sua caixa de entrada.");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponseDTO(token, user.getUsername(), user.getEmail());
    }

    @Transactional
    public MessageResponseDTO resendCode(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        sendNewVerificationCode(email);

        return new MessageResponseDTO("Novo código enviado para " + email);
    }

    private void sendNewVerificationCode(String email) {
        String code = String.format("%04d", new Random().nextInt(10000));

        verificationCodeRepository.deleteByEmail(email);

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationCodeRepository.save(verificationCode);

        emailService.sendVerificationCode(email, code);
    }

}

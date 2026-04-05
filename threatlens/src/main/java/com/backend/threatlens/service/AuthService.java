package com.backend.threatlens.service;

import com.backend.threatlens.dto.request.LoginRequestDTO;
import com.backend.threatlens.dto.request.RegisterRequestDTO;
import com.backend.threatlens.dto.response.AuthResponseDTO;
import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.repository.UserRepository;
import com.backend.threatlens.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthResponseDTO register(RegisterRequestDTO  registerRequestDTO) {
        if (userRepository.findByEmail(registerRequestDTO.email()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        UserEntity user = new UserEntity();
        user.setUsername(registerRequestDTO.username());
        user.setEmail(registerRequestDTO.email());
        user.setPassword(passwordEncoder.encode(registerRequestDTO.password()));
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponseDTO(token,user.getUsername(), user.getEmail());
    }

    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequestDTO.email(),
                loginRequestDTO.password()
        ));

        UserEntity user = userRepository.findByEmail(loginRequestDTO.email())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + loginRequestDTO.email()));

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponseDTO(token,user.getUsername(), user.getEmail());
    }
}

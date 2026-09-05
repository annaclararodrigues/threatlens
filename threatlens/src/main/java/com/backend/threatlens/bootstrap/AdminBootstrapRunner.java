package com.backend.threatlens.bootstrap;

import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.enums.Role;
import com.backend.threatlens.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.email:}")
    private String adminEmail;

    @Value("${admin.bootstrap.username:admin}")
    private String adminUsername;

    @Value("${admin.bootstrap.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("Nenhum administrador encontrado e ADMIN_EMAIL/ADMIN_PASSWORD não foram configurados. " +
                    "Defina essas variáveis de ambiente para criar o primeiro administrador.");
            return;
        }

        UserEntity admin = UserEntity.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .isEmailVerified(true)
                .build();

        userRepository.save(admin);
        log.info("Administrador inicial criado para o e-mail {}", adminEmail);
    }
}

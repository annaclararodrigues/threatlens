package com.backend.threatlens.service;

import com.backend.threatlens.dto.response.PageResponseDTO;
import com.backend.threatlens.dto.response.UserSummaryResponseDTO;
import com.backend.threatlens.entity.UserEntity;
import com.backend.threatlens.enums.Role;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.exception.ResourceNotFoundException;
import com.backend.threatlens.repository.RefreshTokenRepository;
import com.backend.threatlens.repository.UserRepository;
import com.backend.threatlens.repository.VerificationCodeRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @InjectMocks
    private AdminService adminService;

    @Nested
    class ListUsers {

        @Nested
        class SearchNormalization {

            @Test
            void nullSearch_passesNullToRepository() {
                Pageable pageable = PageRequest.of(0, 20);
                when(userRepository.findAllWithFilters(null, null, pageable)).thenReturn(Page.empty(pageable));

                adminService.listUsers(null, null, pageable);

                verify(userRepository).findAllWithFilters(null, null, pageable);
            }

            @Test
            void blankSearch_treatedAsNull() {
                Pageable pageable = PageRequest.of(0, 20);
                when(userRepository.findAllWithFilters(null, null, pageable)).thenReturn(Page.empty(pageable));

                adminService.listUsers(null, "   ", pageable);

                verify(userRepository).findAllWithFilters(null, null, pageable);
            }

            @Test
            void nonBlankSearch_normalizesToLowercasePatternWithWildcards() {
                Pageable pageable = PageRequest.of(0, 20);
                ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
                when(userRepository.findAllWithFilters(isNull(), searchCaptor.capture(), eq(pageable)))
                        .thenReturn(Page.empty(pageable));

                adminService.listUsers(null, "Anna", pageable);

                assertThat(searchCaptor.getValue()).isEqualTo("%anna%");
            }

            @Test
            void searchWithPercent_escapesWildcard() {
                Pageable pageable = PageRequest.of(0, 20);
                ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
                when(userRepository.findAllWithFilters(isNull(), searchCaptor.capture(), eq(pageable)))
                        .thenReturn(Page.empty(pageable));

                adminService.listUsers(null, "50%", pageable);

                assertThat(searchCaptor.getValue()).isEqualTo("%50\\%%");
            }

            @Test
            void searchWithUnderscore_escapesWildcard() {
                Pageable pageable = PageRequest.of(0, 20);
                ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
                when(userRepository.findAllWithFilters(isNull(), searchCaptor.capture(), eq(pageable)))
                        .thenReturn(Page.empty(pageable));

                adminService.listUsers(null, "user_name", pageable);

                assertThat(searchCaptor.getValue()).isEqualTo("%user\\_name%");
            }
        }

        @Nested
        class Filtering {

            @Test
            void withRole_passesRoleToRepository() {
                Pageable pageable = PageRequest.of(0, 20);
                when(userRepository.findAllWithFilters(eq(Role.ADMIN), isNull(), eq(pageable)))
                        .thenReturn(Page.empty(pageable));

                adminService.listUsers(Role.ADMIN, null, pageable);

                verify(userRepository).findAllWithFilters(Role.ADMIN, null, pageable);
            }
        }

        @Nested
        class Mapping {

            @Test
            void mapsAllEntityFieldsToDTO() {
                UUID id = UUID.randomUUID();
                UserEntity entity = buildUser(id, "Anna", "anna@test.com", Role.ADMIN, true);
                Pageable pageable = PageRequest.of(0, 20);
                when(userRepository.findAllWithFilters(null, null, pageable))
                        .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

                PageResponseDTO<UserSummaryResponseDTO> result = adminService.listUsers(null, null, pageable);

                assertThat(result.content()).hasSize(1);
                UserSummaryResponseDTO dto = result.content().getFirst();
                assertThat(dto.id()).isEqualTo(id);
                assertThat(dto.username()).isEqualTo("Anna");
                assertThat(dto.email()).isEqualTo("anna@test.com");
                assertThat(dto.role()).isEqualTo(Role.ADMIN);
                assertThat(dto.isEmailVerified()).isTrue();
            }

            @Test
            void paginationMetadata_mappedCorrectly() {
                // page 1 of 5 (size=2, total=10): offset(2)+size(2)=4 < 10, so Spring Data keeps total=10
                Pageable pageable = PageRequest.of(1, 2);
                Page<UserEntity> page = new PageImpl<>(
                        List.of(
                                buildUser(UUID.randomUUID(), "u1", "u1@t.com", Role.USER, false),
                                buildUser(UUID.randomUUID(), "u2", "u2@t.com", Role.USER, false)
                        ),
                        pageable, 10
                );
                when(userRepository.findAllWithFilters(null, null, pageable)).thenReturn(page);

                PageResponseDTO<UserSummaryResponseDTO> result = adminService.listUsers(null, null, pageable);

                assertThat(result.page()).isEqualTo(1);
                assertThat(result.size()).isEqualTo(2);
                assertThat(result.totalElements()).isEqualTo(10);
                assertThat(result.totalPages()).isEqualTo(5);
                assertThat(result.last()).isFalse();
            }
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void userNotFound_throwsResourceNotFoundException() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteUser(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuário não encontrado.");
        }

        @Test
        void lastAdmin_throwsLastAdminDeletionNotAllowedException() {
            UUID id = UUID.randomUUID();
            UserEntity admin = buildUser(id, "admin", "admin@test.com", Role.ADMIN, true);
            when(userRepository.findById(id)).thenReturn(Optional.of(admin));
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

            assertThatThrownBy(() -> adminService.deleteUser(id))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessage("Não é permitido remover o último administrador.");
        }

        @Test
        void adminUser_withOtherAdmins_deletesUserAndCleansUpTokens() {
            UUID id = UUID.randomUUID();
            UserEntity admin = buildUser(id, "admin", "admin@test.com", Role.ADMIN, true);
            when(userRepository.findById(id)).thenReturn(Optional.of(admin));
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);

            adminService.deleteUser(id);

            verify(refreshTokenRepository).deleteByEmail("admin@test.com");
            verify(verificationCodeRepository).deleteByEmail("admin@test.com");
            verify(userRepository).delete(admin);
        }

        @Test
        void regularUser_deletesUserAndCleansUpTokens() {
            UUID id = UUID.randomUUID();
            UserEntity user = buildUser(id, "anna", "anna@test.com", Role.USER, true);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            adminService.deleteUser(id);

            verify(refreshTokenRepository).deleteByEmail("anna@test.com");
            verify(verificationCodeRepository).deleteByEmail("anna@test.com");
            verify(userRepository).delete(user);
        }
    }

    @Nested
    class UpdateUserRole {

        @Test
        void userNotFound_throwsResourceNotFoundException() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateUserRole(id, Role.USER))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuário não encontrado.");
        }

        @Test
        void lastAdmin_demotedToUser_throwsBusinessRuleViolationException() {
            UUID id = UUID.randomUUID();
            UserEntity admin = buildUser(id, "admin", "admin@test.com", Role.ADMIN, true);
            when(userRepository.findById(id)).thenReturn(Optional.of(admin));
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

            assertThatThrownBy(() -> adminService.updateUserRole(id, Role.USER))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessage("Não é permitido remover o último administrador.");

            verify(userRepository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        void adminWithOtherAdmins_demotedToUser_succeeds() {
            UUID id = UUID.randomUUID();
            UserEntity admin = buildUser(id, "admin", "admin@test.com", Role.ADMIN, true);
            when(userRepository.findById(id)).thenReturn(Optional.of(admin));
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);

            adminService.updateUserRole(id, Role.USER);

            assertThat(admin.getRole()).isEqualTo(Role.USER);
            verify(userRepository).save(admin);
        }

        @Test
        void regularUser_promotedToAdmin_succeeds() {
            UUID id = UUID.randomUUID();
            UserEntity user = buildUser(id, "anna", "anna@test.com", Role.USER, true);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            adminService.updateUserRole(id, Role.ADMIN);

            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
            verify(userRepository).save(user);
        }
    }

    private UserEntity buildUser(UUID id, String username, String email, Role role, boolean emailVerified) {
        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .password("hashed")
                .role(role)
                .build();
        user.setId(id);
        user.setEmailVerified(emailVerified);
        return user;
    }
}

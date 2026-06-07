package com.backend.threatlens.controller;

import com.backend.threatlens.dto.response.PageResponseDTO;
import com.backend.threatlens.dto.response.UserSummaryResponseDTO;
import com.backend.threatlens.enums.Role;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.exception.GlobalExceptionHandler;
import com.backend.threatlens.exception.ResourceNotFoundException;
import com.backend.threatlens.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .build();
    }

    @Nested
    class ListUsers {

        @Nested
        class RequestParams {

            @Test
            void noParams_callsServiceWithNullFilters() throws Exception {
                when(adminService.listUsers(any(), any(), any())).thenReturn(emptyPage());

                mockMvc.perform(get("/admin/users"))
                        .andExpect(status().isOk());

                verify(adminService).listUsers(isNull(), isNull(), any(Pageable.class));
            }

            @Test
            void withSearchParam_forwardsRawValueToService() throws Exception {
                when(adminService.listUsers(any(), any(), any())).thenReturn(emptyPage());

                mockMvc.perform(get("/admin/users").param("search", "anna"))
                        .andExpect(status().isOk());

                ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
                verify(adminService).listUsers(isNull(), searchCaptor.capture(), any(Pageable.class));
                assertThat(searchCaptor.getValue()).isEqualTo("anna");
            }

            @Test
            void withRoleParam_convertsEnumAndForwardsToService() throws Exception {
                when(adminService.listUsers(any(), any(), any())).thenReturn(emptyPage());

                mockMvc.perform(get("/admin/users").param("role", "ADMIN"))
                        .andExpect(status().isOk());

                ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
                verify(adminService).listUsers(roleCaptor.capture(), isNull(), any(Pageable.class));
                assertThat(roleCaptor.getValue()).isEqualTo(Role.ADMIN);
            }

            @Test
            void withPageableParams_forwardsToService() throws Exception {
                when(adminService.listUsers(any(), any(), any())).thenReturn(emptyPage());

                mockMvc.perform(get("/admin/users")
                        .param("page", "1")
                        .param("size", "10"))
                        .andExpect(status().isOk());

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                verify(adminService).listUsers(isNull(), isNull(), pageableCaptor.capture());
                assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
                assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
            }
        }

        @Nested
        class ResponseBody {

            @Test
            void returnsPageResponseDTOShape() throws Exception {
                PageResponseDTO<UserSummaryResponseDTO> response = new PageResponseDTO<>(
                        List.of(new UserSummaryResponseDTO(
                                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                "Anna", "anna@test.com", Role.ADMIN, true)),
                        0, 20, 1L, 1, true
                );
                when(adminService.listUsers(any(), any(), any())).thenReturn(response);

                mockMvc.perform(get("/admin/users"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].username").value("Anna"))
                        .andExpect(jsonPath("$.content[0].email").value("anna@test.com"))
                        .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.totalPages").value(1))
                        .andExpect(jsonPath("$.page").value(0))
                        .andExpect(jsonPath("$.last").value(true));
            }
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void validId_returns204() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(adminService).deleteUser(id);

            mockMvc.perform(delete("/admin/users/{id}", id))
                    .andExpect(status().isNoContent());

            verify(adminService).deleteUser(id);
        }

        @Test
        void userNotFound_returns404WithMessage() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Usuário não encontrado."))
                    .when(adminService).deleteUser(id);

            mockMvc.perform(delete("/admin/users/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Usuário não encontrado."));
        }

        @Test
        void adminUser_returns422WithMessage() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new BusinessRuleViolationException("Não é permitido remover um administrador."))
                    .when(adminService).deleteUser(id);

            mockMvc.perform(delete("/admin/users/{id}", id))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.message").value("Não é permitido remover um administrador."));
        }
    }

    @Nested
    class Security {

        @Test
        void controllerIsRestrictedToAdminRole() {
            PreAuthorize annotation = AdminController.class.getAnnotation(PreAuthorize.class);
            assertThat(annotation)
                    .as("AdminController deve ter @PreAuthorize declarado")
                    .isNotNull();
            assertThat(annotation.value())
                    .as("A expressão de autorização deve exigir role ADMIN")
                    .isEqualTo("hasRole('ADMIN')");
        }
    }

    private PageResponseDTO<UserSummaryResponseDTO> emptyPage() {
        return new PageResponseDTO<>(List.of(), 0, 20, 0L, 0, true);
    }
}
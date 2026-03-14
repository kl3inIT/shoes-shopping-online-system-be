package com.sba.ssos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sba.ssos.dto.response.user.AdminUserStatsResponse;
import com.sba.ssos.entity.User;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.enums.UserStatus;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.service.keycloak.KeycloakAdminService;
import com.sba.ssos.service.storage.MinioStorageService;
import com.sba.ssos.service.user.AdminUserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @Mock
    private MinioStorageService minioStorageService;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void testGetUserStats() {
        when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(2L);
        when(userRepository.countByRole(UserRole.ROLE_MANAGER)).thenReturn(3L);
        when(userRepository.countByRole(UserRole.ROLE_CUSTOMER)).thenReturn(10L);

        AdminUserStatsResponse stats = adminUserService.getUserStats();

        assertThat(stats.admins()).isEqualTo(2L);
        assertThat(stats.managers()).isEqualTo(3L);
        assertThat(stats.customers()).isEqualTo(10L);
        assertThat(stats.total()).isEqualTo(15L);
    }

    @Test
    void getUsersReturnsNullAvatarWhenStoredAvatarKeyIsBlank() {
        User user = User.builder()
            .keycloakId(UUID.randomUUID())
            .username("manager1")
            .firstName("Store")
            .lastName("Manager")
            .email("manager1@example.com")
            .avatarUrl("   ")
            .role(UserRole.ROLE_MANAGER)
            .status(UserStatus.ACTIVE)
            .build();
        user.setId(UUID.randomUUID());
        user.setCreatedAt(Instant.parse("2026-03-15T00:00:00Z"));

        when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user)));

        var response = adminUserService.getUsers(0, 10, null, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().avatarUrl()).isNull();
        verifyNoInteractions(minioStorageService);
    }
}

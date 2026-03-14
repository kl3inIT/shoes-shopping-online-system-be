package com.sba.ssos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sba.ssos.dto.response.user.AdminUserStatsResponse;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.repository.CustomerRepository;
import com.sba.ssos.repository.UserRepository;
import com.sba.ssos.service.keycloak.KeycloakAdminService;
import com.sba.ssos.service.storage.MinioStorageService;
import com.sba.ssos.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}

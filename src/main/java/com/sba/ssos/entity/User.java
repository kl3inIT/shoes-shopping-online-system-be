package com.sba.ssos.entity;

import com.sba.ssos.entity.base.BaseAuditableEntity;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.enums.UserStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_role", columnList = "role")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditableEntity {

    @NaturalId
    @Column(name = "keycloak_id", nullable = false, unique = true, updatable = false)
    private UUID keycloakId;
    
    @Column(name = "role", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(name = "phone_number", nullable = true, length = 255)
    private String phoneNumber;

    @NaturalId
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "date_of_birth", nullable = true)
    private LocalDate dateOfBirth;

    @Column(name = "avatar_url", nullable = true, columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "address", nullable = true, columnDefinition = "TEXT")
    private String address;

    @Column(name = "status", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "LAST_SEEN_AT")
    private Instant lastSeenAt;
}

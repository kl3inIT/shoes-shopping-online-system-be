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
    name = "USERS",
    indexes = {
      @Index(name = "idx_user_email", columnList = "email", unique = true),
      @Index(name = "idx_user_keycloak_id", columnList = "keycloak_id", unique = true),
      @Index(name = "idx_user_username", columnList = "username", unique = true),
      @Index(name = "idx_user_role", columnList = "role"),
      @Index(name = "idx_user_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditableEntity {

  @NaturalId
  @Column(name = "KEYCLOAK_ID", nullable = false, unique = true, updatable = false)
  private UUID keycloakId;

  @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
  private String username;

  @NaturalId
  @Column(name = "EMAIL", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "FIRST_NAME", length = 100)
  private String firstName;

  @Column(name = "LAST_NAME", length = 100)
  private String lastName;

  @Column(name = "PHONE_NUMBER", length = 20)
  private String phoneNumber;

  @Column(name = "DATE_OF_BIRTH")
  private LocalDate dateOfBirth;

  @Column(name = "AVATAR_URL", length = 500)
  private String avatarUrl;

  @Column(name = "LAST_SEEN_AT")
  private Instant lastSeenAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "ROLE", nullable = false, length = 50)
  @Builder.Default
  private UserRole role = UserRole.ROLE_USER;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;
}

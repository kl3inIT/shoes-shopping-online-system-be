//package com.sba.ssos.entity;
//
//import com.sba.ssos.entity.base.BaseAuditableEntity;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.NaturalId;
//
//import java.time.LocalDate;
//
//@Entity
//@Table(name = "USERS", indexes = {
//        @Index(name = "idx_user_email", columnList = "email", unique = true),
//        @Index(name = "idx_user_keycloak_id", columnList = "keycloak_id", unique = true),
//        @Index(name = "idx_user_username", columnList = "username", unique = true)
//})
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class User extends BaseAuditableEntity {
//
//    @NaturalId
//    @Column(name = "KEYCLOAK_ID", nullable = false, unique = true, updatable = false)
//    private String keycloakId;  // Subject từ JWT (UUID string)
//
//    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
//    private String username;
//
//    @NaturalId
//    @Column(name = "EMAIL", nullable = false, unique = true, length = 255)
//    private String email;
//
//    @Column(name = "FIRST_NAME", length = 100)
//    private String firstName;
//
//    @Column(name = "LAST_NAME", length = 100)
//    private String lastName;
//
//    @Column(name = "PHONE_NUMBER", length = 20)
//    private String phoneNumber;
//
//    @Column(name = "DATE_OF_BIRTH")
//    private LocalDate dateOfBirth;
//
//    @Column(name = "AVATAR_URL", length = 500)
//    private String avatarUrl;
//
//}
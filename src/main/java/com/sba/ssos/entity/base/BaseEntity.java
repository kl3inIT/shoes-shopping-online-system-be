package com.sba.ssos.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public class BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "ID", updatable = false, nullable = false)
    private UUID id;

    @Version
    private Long version;
}

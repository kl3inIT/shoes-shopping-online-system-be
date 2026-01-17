package com.sba.ssos.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditableEntity extends BaseEntity {

    @CreatedBy
    @Column(name = "CREATED_BY", updatable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(name = "UPDATED_BY")
    private UUID lastUpdatedBy;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private Instant lastUpdatedAt;
}

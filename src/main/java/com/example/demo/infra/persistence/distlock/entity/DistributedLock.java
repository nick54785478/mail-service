package com.example.demo.infra.persistence.distlock.entity;

import java.time.Instant;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;



@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "DISTRIBUTED_LOCK", uniqueConstraints = { @UniqueConstraint(columnNames = { "lockKey" }) })
@EntityListeners(AuditingEntityListener.class)
public class DistributedLock {

  @Id
  @Column(name = "LOCK_KEY", nullable = false, length = 100)
  private String lockKey;

  @Column(name = "OWNER_ID", nullable = false, length = 100)
  private String ownerId;

  @Column(name = "EXPIRES_AT", nullable = false)
  private Instant expiresAt;

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public DistributedLock(String lockKey, String ownerId, Instant expiresAt) {
    this.lockKey = lockKey;
    this.ownerId = ownerId;
    this.expiresAt = expiresAt;
  }

}

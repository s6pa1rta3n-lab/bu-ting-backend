package com.butingbe.domain.zoneevent.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Audit trail for administrative actions executed on zone events and rounds. */
@Entity
@Table(name = "zone_event_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZoneEventAuditLog extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "log_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "operator_id")
  private UUID operatorId;

  @Column(nullable = false, length = 100)
  private String action;

  @Column(name = "target_type", nullable = false, length = 50)
  private String targetType;

  @Column(name = "target_id", length = 100)
  private String targetId;

  @Column(columnDefinition = "text")
  private String details;

  @Builder
  private ZoneEventAuditLog(
      UUID operatorId, String action, String targetType, String targetId, String details) {
    this.operatorId = operatorId;
    this.action = action;
    this.targetType = targetType;
    this.targetId = targetId;
    this.details = details;
  }
}

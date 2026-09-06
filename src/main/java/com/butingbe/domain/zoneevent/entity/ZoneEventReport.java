package com.butingbe.domain.zoneevent.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing a user report against an event participation. */
@Entity
@Table(
    name = "zone_event_report",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_zone_event_report",
          columnNames = {"participation_id", "reporter_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZoneEventReport extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "report_id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "participation_id", nullable = false)
  private ZoneEventParticipation participation;

  @Column(name = "reporter_id", nullable = false)
  private UUID reporterId;

  @Column(name = "reason_code", nullable = false, length = 50)
  private String reasonCode;

  @Column(name = "reason_detail", columnDefinition = "text")
  private String reasonDetail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReportStatus status;

  @Builder
  private ZoneEventReport(
      ZoneEventParticipation participation,
      UUID reporterId,
      String reasonCode,
      String reasonDetail,
      ReportStatus status) {
    this.participation = participation;
    this.reporterId = reporterId;
    this.reasonCode = reasonCode;
    this.reasonDetail = reasonDetail;
    this.status = status == null ? ReportStatus.OPEN : status;
  }

  /** Resolves the report. */
  public void resolve() {
    this.status = ReportStatus.RESOLVED;
  }

  /** Dismisses the report. */
  public void dismiss() {
    this.status = ReportStatus.DISMISSED;
  }
}

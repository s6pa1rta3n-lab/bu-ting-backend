package com.butingbe.domain.zoneevent.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Snapshot report generated upon round settlement completion. */
@Entity
@Table(name = "zone_event_settlement_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZoneEventSettlementReport extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "report_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "round_id", nullable = false, unique = true)
  private UUID roundId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "summary_json", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> summaryJson;

  @Builder
  private ZoneEventSettlementReport(UUID roundId, Map<String, Object> summaryJson) {
    this.roundId = roundId;
    this.summaryJson = summaryJson;
  }
}

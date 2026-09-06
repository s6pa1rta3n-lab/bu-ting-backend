package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO representing an event participation report. */
public record ReportResDto(
    UUID reportId,
    UUID participationId,
    UUID reporterId,
    String reasonCode,
    String reasonDetail,
    ReportStatus status,
    LocalDateTime createdAt) {

  public static ReportResDto from(ZoneEventReport report) {
    return new ReportResDto(
        report.getId(),
        report.getParticipation().getId(),
        report.getReporterId(),
        report.getReasonCode(),
        report.getReasonDetail(),
        report.getStatus(),
        report.getCreatedAt());
  }
}

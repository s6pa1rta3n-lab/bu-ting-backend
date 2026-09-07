package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.reward.dto.response.AdminPayoutItemResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ReportReasonCode;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 신고 검수 상세 응답 DTO (신고 내용·검수 대상·관련 지급 건).
 *
 * @param report 신고 내용
 * @param target 검수 대상 참여 정보
 * @param relatedPayouts 관련 지급 건 목록
 */
public record AdminReportDetailResDto(
    ReportInfo report, TargetInfo target, List<AdminPayoutItemResDto> relatedPayouts) {

  /** 신고 상세 정보. */
  public record ReportInfo(
      UUID reportId,
      UUID reporterId,
      ReportReasonCode reasonCode,
      String memo,
      ReportStatus status,
      OffsetDateTime createdAt,
      UUID reviewedBy,
      OffsetDateTime reviewedAt,
      String decisionNote,
      Long revision) {

    /** 신고 엔티티로부터 DTO를 생성한다. */
    public static ReportInfo from(ZoneEventReport report) {
      return new ReportInfo(
          report.getId(),
          report.getReporterId(),
          report.getReasonCode(),
          report.getMemo(),
          report.getStatus(),
          report.getCreatedAt(),
          report.getReviewedBy(),
          report.getReviewedAt(),
          report.getDecisionNote(),
          report.getRevision());
    }
  }

  /** 검수 대상 참여 정보. */
  public record TargetInfo(
      UUID participationId,
      UUID eventId,
      UUID roundId,
      UUID userId,
      String mediaFileKey,
      String content,
      ParticipationStatus status,
      UUID currentSubmissionId) {

    /** 참여 엔티티로부터 DTO를 생성한다. */
    public static TargetInfo from(ZoneEventParticipation p) {
      if (p == null) {
        return null;
      }
      return new TargetInfo(
          p.getId(),
          p.getEvent() != null ? p.getEvent().getId() : null,
          p.getEvent() != null ? p.getEvent().getRoundId() : null,
          p.getUserId(),
          p.getMediaFileKey(),
          p.getContent(),
          p.getStatus(),
          p.getCurrentSubmissionId());
    }
  }
}

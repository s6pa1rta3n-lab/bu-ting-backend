package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.reward.entity.PayoutHoldStatus;
import com.butingbe.domain.zoneevent.entity.ReportReasonCode;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 신고 목록 요약 응답 DTO.
 *
 * @param reportId 신고 식별자
 * @param reporterId 신고자 식별자
 * @param reasonCode 신고 사유 코드
 * @param memo 신고 메모
 * @param status 신고 상태
 * @param createdAt 신고 일시
 * @param reviewedBy 검수자 식별자
 * @param reviewedAt 검수 일시
 * @param decisionNote 검수 메모
 * @param revision 리비전
 * @param participationId 대상 참여 식별자
 * @param eventId 이벤트 식별자
 * @param roundId 회차 식별자
 * @param userId 참여자 식별자
 * @param holdStatus 지급 보류 상태
 */
public record AdminReportSummaryResDto(
    UUID reportId,
    UUID reporterId,
    ReportReasonCode reasonCode,
    String memo,
    ReportStatus status,
    OffsetDateTime createdAt,
    UUID reviewedBy,
    OffsetDateTime reviewedAt,
    String decisionNote,
    Long revision,
    UUID participationId,
    UUID eventId,
    UUID roundId,
    UUID userId,
    PayoutHoldStatus holdStatus) {}

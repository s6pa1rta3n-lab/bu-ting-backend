package com.butingbe.domain.zoneevent.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Top N 후보 단건 응답.
 *
 * @param snapshotId 스냅샷 식별자
 * @param participationId 참여 식별자
 * @param userId 참여자 사용자 식별자
 * @param rankN 순위
 * @param likeCount 마감 시점 좋아요 수
 * @param tied 경계 동점 여부
 * @param finalized 확정 여부
 * @param heldByReport 미해결 신고로 인한 보류 여부
 * @param reportCount 미해결 신고 수
 * @param mediaFileKey 인증 미디어 파일 키
 * @param submittedAt 제출 시점
 */
public record TopNCandidateResDto(
    UUID snapshotId,
    UUID participationId,
    UUID userId,
    Integer rankN,
    Long likeCount,
    Boolean tied,
    Boolean finalized,
    Boolean heldByReport,
    Long reportCount,
    String mediaFileKey,
    OffsetDateTime submittedAt) {}

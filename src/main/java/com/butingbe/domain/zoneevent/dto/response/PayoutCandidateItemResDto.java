package com.butingbe.domain.zoneevent.dto.response;

import java.util.UUID;

/**
 * 지급 후보 항목 단건 응답.
 *
 * @param payoutId 지급 식별자
 * @param participationId 참여 식별자
 * @param rankN 순위
 * @param likeCountAtClose 마감 시점 좋아요 수
 * @param status 지급 상태
 * @param holdStatus 보류 상태
 */
public record PayoutCandidateItemResDto(
    UUID payoutId,
    UUID participationId,
    Integer rankN,
    Long likeCountAtClose,
    String status,
    String holdStatus) {}

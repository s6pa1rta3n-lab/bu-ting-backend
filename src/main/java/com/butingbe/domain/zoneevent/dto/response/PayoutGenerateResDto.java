package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 지급 후보 생성 결과 응답.
 *
 * @param eventId 이벤트 식별자
 * @param totalGenerated 새로 생성된 지급 건수
 * @param heldCount 신고 등으로 보류 처리된 건수
 * @param payouts 지급 후보 목록
 */
public record PayoutGenerateResDto(
    UUID eventId, int totalGenerated, int heldCount, List<PayoutCandidateItemResDto> payouts) {}

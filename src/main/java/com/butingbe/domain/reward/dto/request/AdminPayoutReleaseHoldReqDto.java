package com.butingbe.domain.reward.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 보상 지급 보류 해제 요청 DTO.
 *
 * @param note 최종 검수 근거
 * @param expectedRevision 낙관적 락 검증용 리비전
 */
public record AdminPayoutReleaseHoldReqDto(@Size(max = 500) String note, Long expectedRevision) {}

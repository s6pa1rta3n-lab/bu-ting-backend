package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 신고 인정(UPHELD) 처리 요청 DTO.
 *
 * @param note 검수 사유 및 메모
 * @param action 후속 조치 유형 (예: HOLD 등)
 * @param expectedRevision 낙관적 락 검증용 리비전
 */
public record AdminReportUpholdReqDto(
    @Size(max = 500) String note, String action, Long expectedRevision) {}

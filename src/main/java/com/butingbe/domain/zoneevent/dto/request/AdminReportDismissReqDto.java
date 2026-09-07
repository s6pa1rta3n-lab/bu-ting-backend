package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 신고 기각(DISMISSED) 처리 요청 DTO.
 *
 * @param note 검수 사유 및 메모
 * @param expectedRevision 낙관적 락 검증용 리비전
 */
public record AdminReportDismissReqDto(@Size(max = 500) String note, Long expectedRevision) {}

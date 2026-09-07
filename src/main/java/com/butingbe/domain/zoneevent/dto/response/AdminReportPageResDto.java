package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;

/**
 * 신고 목록 페이징 응답 DTO.
 *
 * @param items 신고 요약 목록
 * @param page 현재 페이지 번호 (0부터 시작)
 * @param size 페이지당 개수
 * @param totalElements 전체 항목 수
 * @param totalPages 전체 페이지 수
 */
public record AdminReportPageResDto(
    List<AdminReportSummaryResDto> items, int page, int size, long totalElements, int totalPages) {}

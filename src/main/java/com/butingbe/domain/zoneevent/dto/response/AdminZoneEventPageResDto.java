package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;

/**
 * 관리자 구역 이벤트 목록 페이징 응답 DTO.
 *
 * @param items 이벤트 요약 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages 전체 페이지 수
 */
public record AdminZoneEventPageResDto(
    List<ZoneEventSummaryResDto> items, int page, int size, long totalElements, int totalPages) {}

package com.butingbe.domain.reward.dto.response;

import java.util.List;

/**
 * 포인트 원장 내역 커서 페이징 응답.
 *
 * @param items 원장 항목 목록
 * @param nextCursor 다음 커서 문자열
 * @param hasNext 다음 페이지 존재 여부
 */
public record PointLedgerPageResDto(
    List<PointLedgerEntryResDto> items, String nextCursor, boolean hasNext) {}

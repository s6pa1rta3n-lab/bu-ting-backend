package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;

/**
 * 유저의 구역 이벤트 참여 내역 커서 페이징 응답.
 *
 * @param items 참여 목록
 * @param nextCursor 다음 커서 문자열
 * @param hasNext 다음 페이지 존재 여부
 */
public record ZoneEventParticipationPageResDto(
    List<ParticipationResDto> items, String nextCursor, boolean hasNext) {}

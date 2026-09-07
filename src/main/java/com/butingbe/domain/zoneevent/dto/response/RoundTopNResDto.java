package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 회차별 Top N 및 경계 동점 후보 목록 응답.
 *
 * @param roundId 회차 식별자
 * @param events 이벤트별 Top N 후보 목록
 */
public record RoundTopNResDto(UUID roundId, List<EventTopNResDto> events) {}

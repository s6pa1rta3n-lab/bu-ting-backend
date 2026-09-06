package com.butingbe.domain.reward.dto.response;

import java.util.List;

/**
 * 6대 구역별 배지 월 그룹 응답.
 *
 * @param zoneId 구역 식별자 (ChatZone enum 이름)
 * @param zoneName 구역 한글명
 * @param badges 해당 구역에서 획득한 배지 목록
 */
public record ZoneBadgeGroupResDto(
    String zoneId, String zoneName, List<UserBadgeItemResDto> badges) {}

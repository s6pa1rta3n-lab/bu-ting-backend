package com.butingbe.domain.reward.dto.response;

import java.time.OffsetDateTime;

/**
 * 유저 획득 배지 단건 응답.
 *
 * @param badgeId 배지 획득 식별자
 * @param code 배지 코드
 * @param name 배지 이름
 * @param imageUrl 배지 이미지 사전 서명 URL
 * @param earnedAt 획득 일시
 */
public record UserBadgeItemResDto(
    String badgeId, String code, String name, String imageUrl, OffsetDateTime earnedAt) {}

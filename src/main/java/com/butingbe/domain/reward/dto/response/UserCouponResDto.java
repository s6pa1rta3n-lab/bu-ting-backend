package com.butingbe.domain.reward.dto.response;

import java.time.OffsetDateTime;

/**
 * 유저 보유 쿠폰/기프티콘 단건 응답.
 *
 * @param grantId 지급 식별자
 * @param code 보상 카탈로그 코드
 * @param name 보상 이름
 * @param couponCode 쿠폰 표시 코드
 * @param validUntil 유효 기간 만료 일시
 * @param grantedAt 지급 일시
 * @param used 사용/회수 여부
 */
public record UserCouponResDto(
    String grantId,
    String code,
    String name,
    String couponCode,
    OffsetDateTime validUntil,
    OffsetDateTime grantedAt,
    boolean used) {}

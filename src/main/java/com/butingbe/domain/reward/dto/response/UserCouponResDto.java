package com.butingbe.domain.reward.dto.response;

import com.butingbe.domain.reward.entity.CouponStatus;
import com.butingbe.domain.reward.entity.UserCoupon;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 유저 쿠폰함 항목 응답. */
public record UserCouponResDto(
    UUID couponId,
    UUID rewardId,
    String rewardCode,
    String rewardName,
    String couponCode,
    CouponStatus status,
    String imageUrl,
    OffsetDateTime expiresAt,
    OffsetDateTime issuedAt,
    OffsetDateTime usedAt) {

  public static UserCouponResDto of(UserCoupon coupon, String imageUrl) {
    return new UserCouponResDto(
        coupon.getId(),
        coupon.getReward().getId(),
        coupon.getReward().getCode(),
        coupon.getReward().getName(),
        coupon.getCouponCode(),
        coupon.getStatus(),
        imageUrl,
        coupon.getExpiresAt(),
        coupon.getIssuedAt(),
        coupon.getUsedAt());
  }
}

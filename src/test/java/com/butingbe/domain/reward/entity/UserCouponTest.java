package com.butingbe.domain.reward.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.butingbe.global.error.exception.ConflictException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserCouponTest {

  @Test
  @DisplayName("정상 쿠폰 사용 시 상태가 USED로 변경되고 사용 시각이 기록된다")
  void useSuccess() {
    RewardCatalog reward = RewardCatalog.builder().code("COFFEE").build();
    UserCoupon coupon =
        UserCoupon.builder()
            .userId(UUID.randomUUID())
            .reward(reward)
            .grantId(UUID.randomUUID())
            .couponCode("CPN-1111")
            .status(CouponStatus.ISSUED)
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .build();

    OffsetDateTime now = OffsetDateTime.now();
    coupon.use(now);

    assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
    assertThat(coupon.getUsedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("만료일이 없는 정상 쿠폰도 정상적으로 사용된다")
  void useSuccessWithoutExpiration() {
    RewardCatalog reward = RewardCatalog.builder().code("COFFEE").build();
    UserCoupon coupon =
        UserCoupon.builder()
            .userId(UUID.randomUUID())
            .reward(reward)
            .grantId(UUID.randomUUID())
            .couponCode("CPN-1111")
            .status(CouponStatus.ISSUED)
            .expiresAt(null)
            .build();

    OffsetDateTime now = OffsetDateTime.now();
    coupon.use(now);

    assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
    assertThat(coupon.getUsedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("이미 사용된 쿠폰 사용 시 ConflictException이 발생한다")
  void useAlreadyUsed() {
    RewardCatalog reward = RewardCatalog.builder().code("COFFEE").build();
    UserCoupon coupon =
        UserCoupon.builder()
            .userId(UUID.randomUUID())
            .reward(reward)
            .grantId(UUID.randomUUID())
            .couponCode("CPN-1111")
            .status(CouponStatus.USED)
            .build();

    assertThatThrownBy(() -> coupon.use(OffsetDateTime.now()))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.coupon.already_used");
  }

  @Test
  @DisplayName("만료된 쿠폰 사용 시 상태가 EXPIRED로 갱신되고 ConflictException이 발생한다")
  void useExpired() {
    RewardCatalog reward = RewardCatalog.builder().code("COFFEE").build();
    UserCoupon coupon =
        UserCoupon.builder()
            .userId(UUID.randomUUID())
            .reward(reward)
            .grantId(UUID.randomUUID())
            .couponCode("CPN-1111")
            .status(CouponStatus.ISSUED)
            .expiresAt(OffsetDateTime.now().minusDays(1))
            .build();

    assertThatThrownBy(() -> coupon.use(OffsetDateTime.now()))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.coupon.expired");

    assertThat(coupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
  }

  @Test
  @DisplayName("expire 호출 시 상태가 EXPIRED로 전환된다")
  void expire() {
    UserCoupon coupon =
        UserCoupon.builder()
            .userId(UUID.randomUUID())
            .couponCode("CPN-1111")
            .status(CouponStatus.ISSUED)
            .build();

    coupon.expire();
    assertThat(coupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
  }

  @Test
  @DisplayName("isUsable은 발급 상태이고 유효기간 내일 때만 참을 반환한다")
  void isUsable() {
    OffsetDateTime now = OffsetDateTime.now();
    UserCoupon valid =
        UserCoupon.builder().status(CouponStatus.ISSUED).expiresAt(now.plusMinutes(10)).build();
    UserCoupon validNoExpiry =
        UserCoupon.builder().status(CouponStatus.ISSUED).expiresAt(null).build();
    UserCoupon expired =
        UserCoupon.builder().status(CouponStatus.ISSUED).expiresAt(now.minusMinutes(10)).build();
    UserCoupon used =
        UserCoupon.builder().status(CouponStatus.USED).expiresAt(now.plusMinutes(10)).build();

    assertThat(valid.isUsable(now)).isTrue();
    assertThat(validNoExpiry.isUsable(now)).isTrue();
    assertThat(expired.isUsable(now)).isFalse();
    assertThat(used.isUsable(now)).isFalse();
  }
}

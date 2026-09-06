package com.butingbe.domain.reward.entity;

import com.butingbe.global.common.TimestampEntity;
import com.butingbe.global.error.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저가 보유한 실물 쿠폰·기프티콘. */
@Entity
@Table(name = "user_coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "coupon_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reward_id", nullable = false)
  private RewardCatalog reward;

  @Column(name = "grant_id", nullable = false)
  private UUID grantId;

  @Column(name = "coupon_code", nullable = false, length = 100)
  private String couponCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CouponStatus status;

  @Column(name = "issued_at", nullable = false)
  private OffsetDateTime issuedAt;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Column(name = "used_at")
  private OffsetDateTime usedAt;

  @Builder
  private UserCoupon(
      UUID userId,
      RewardCatalog reward,
      UUID grantId,
      String couponCode,
      CouponStatus status,
      OffsetDateTime issuedAt,
      OffsetDateTime expiresAt) {
    this.userId = userId;
    this.reward = reward;
    this.grantId = grantId;
    this.couponCode = couponCode;
    this.status = status != null ? status : CouponStatus.ISSUED;
    this.issuedAt = issuedAt == null ? OffsetDateTime.now() : issuedAt;
    this.expiresAt = expiresAt;
  }

  /** 쿠폰을 사용 처리한다. 이미 사용되었거나 만료된 경우 예외를 발생시킨다. */
  public void use(OffsetDateTime at) {
    if (this.status == CouponStatus.USED) {
      throw new ConflictException("error.coupon.already_used");
    }
    if (this.status == CouponStatus.EXPIRED) {
      throw new ConflictException("error.coupon.expired");
    }
    if (this.expiresAt != null && this.expiresAt.isBefore(at)) {
      this.status = CouponStatus.EXPIRED;
      throw new ConflictException("error.coupon.expired");
    }
    this.status = CouponStatus.USED;
    this.usedAt = at;
  }

  /** 쿠폰을 만료 처리한다. */
  public void expire() {
    this.status = CouponStatus.EXPIRED;
  }

  /** 사용 가능한 상태인지 확인한다. */
  public boolean isUsable(OffsetDateTime now) {
    if (this.status != CouponStatus.ISSUED) {
      return false;
    }
    return this.expiresAt == null || !this.expiresAt.isBefore(now);
  }
}

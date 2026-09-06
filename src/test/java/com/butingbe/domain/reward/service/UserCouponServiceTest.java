package com.butingbe.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.dto.response.UserCouponResDto;
import com.butingbe.domain.reward.entity.CouponStatus;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.entity.UserCoupon;
import com.butingbe.domain.reward.repository.UserCouponRepository;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCouponServiceTest {

  @Mock private UserCouponRepository userCouponRepository;
  @Mock private FileStorageService fileStorageService;
  @InjectMocks private UserCouponService userCouponService;

  private final UUID userId = UUID.randomUUID();
  private final AuthenticatedUser user =
      new AuthenticatedUser(userId, "u@example.com", "u", List.of());

  @Test
  @DisplayName("status가 null이면 전체 쿠폰을 조회한다")
  void getMyCouponsAll() {
    RewardCatalog reward =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN")
            .name("쿠폰")
            .imageFileKey("keys/coupon.png")
            .build();
    UserCoupon coupon =
        UserCoupon.builder()
            .userId(userId)
            .reward(reward)
            .couponCode("CPN-1111")
            .status(CouponStatus.ISSUED)
            .build();

    when(userCouponRepository.findByUserIdOrderByIssuedAtDesc(userId)).thenReturn(List.of(coupon));
    when(fileStorageService.getPresignedUrl("keys/coupon.png"))
        .thenReturn("https://signed/coupon.png");

    List<UserCouponResDto> result = userCouponService.getMyCoupons(user, null);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).couponCode()).isEqualTo("CPN-1111");
    assertThat(result.get(0).imageUrl()).isEqualTo("https://signed/coupon.png");
  }

  @Test
  @DisplayName("status가 제공되면 해당 상태의 쿠폰만 조회한다")
  void getMyCouponsWithStatus() {
    RewardCatalog reward =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN")
            .name("쿠폰")
            .imageFileKey(null)
            .build();
    UserCoupon coupon =
        UserCoupon.builder()
            .userId(userId)
            .reward(reward)
            .couponCode("CPN-2222")
            .status(CouponStatus.USED)
            .build();

    when(userCouponRepository.findByUserIdAndStatusOrderByIssuedAtDesc(userId, CouponStatus.USED))
        .thenReturn(List.of(coupon));

    List<UserCouponResDto> result = userCouponService.getMyCoupons(user, CouponStatus.USED);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).couponCode()).isEqualTo("CPN-2222");
    assertThat(result.get(0).imageUrl()).isNull();
  }

  @Test
  @DisplayName("쿠폰 사용이 성공하면 상태가 USED로 변경된다")
  void useCouponSuccess() {
    UUID couponId = UUID.randomUUID();
    RewardCatalog reward =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN")
            .name("쿠폰")
            .imageFileKey("keys/coupon.png")
            .build();
    UserCoupon coupon =
        UserCoupon.builder()
            .userId(userId)
            .reward(reward)
            .couponCode("CPN-1111")
            .status(CouponStatus.ISSUED)
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .build();

    when(userCouponRepository.findByIdAndUserId(couponId, userId)).thenReturn(Optional.of(coupon));
    when(fileStorageService.getPresignedUrl("keys/coupon.png"))
        .thenReturn("https://signed/coupon.png");

    UserCouponResDto result = userCouponService.useCoupon(user, couponId);
    assertThat(result.status()).isEqualTo(CouponStatus.USED);
  }

  @Test
  @DisplayName("쿠폰이 없으면 ResourceNotFoundException이 발생한다")
  void useCouponNotFound() {
    UUID couponId = UUID.randomUUID();
    when(userCouponRepository.findByIdAndUserId(couponId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userCouponService.useCoupon(user, couponId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("error.coupon.not_found");
  }

  @Test
  @DisplayName("인증되지 않은 유저가 요청하면 UnauthenticatedException이 발생한다")
  void unauthenticated() {
    assertThatThrownBy(() -> userCouponService.getMyCoupons(null, null))
        .isInstanceOf(UnauthenticatedException.class);

    AuthenticatedUser emptyUser = new AuthenticatedUser(null, null, null, List.of());
    assertThatThrownBy(() -> userCouponService.getMyCoupons(emptyUser, null))
        .isInstanceOf(UnauthenticatedException.class);
  }
}

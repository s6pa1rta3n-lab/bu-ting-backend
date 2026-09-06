package com.butingbe.domain.reward.service;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.dto.response.UserCouponResDto;
import com.butingbe.domain.reward.entity.CouponStatus;
import com.butingbe.domain.reward.entity.UserCoupon;
import com.butingbe.domain.reward.repository.UserCouponRepository;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 쿠폰함 서비스. */
@Service
@RequiredArgsConstructor
public class UserCouponService {

  private final UserCouponRepository userCouponRepository;
  private final FileStorageService fileStorageService;

  /** 유저의 쿠폰 목록을 상태별로 조회한다. */
  @Transactional(readOnly = true)
  public List<UserCouponResDto> getMyCoupons(AuthenticatedUser user, CouponStatus status) {
    UUID userId = requireUserId(user);
    List<UserCoupon> coupons =
        status != null
            ? userCouponRepository.findByUserIdAndStatusOrderByIssuedAtDesc(userId, status)
            : userCouponRepository.findByUserIdOrderByIssuedAtDesc(userId);

    return coupons.stream()
        .map(
            coupon ->
                UserCouponResDto.of(coupon, presignedUrl(coupon.getReward().getImageFileKey())))
        .toList();
  }

  /** 쿠폰을 사용 처리한다. */
  @Transactional
  public UserCouponResDto useCoupon(AuthenticatedUser user, UUID couponId) {
    UUID userId = requireUserId(user);
    UserCoupon coupon =
        userCouponRepository
            .findByIdAndUserId(couponId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("error.coupon.not_found"));

    coupon.use(OffsetDateTime.now());
    return UserCouponResDto.of(coupon, presignedUrl(coupon.getReward().getImageFileKey()));
  }

  private String presignedUrl(String fileKey) {
    return fileKey == null ? null : fileStorageService.getPresignedUrl(fileKey);
  }

  private UUID requireUserId(AuthenticatedUser user) {
    if (user == null || user.id() == null) {
      throw new UnauthenticatedException();
    }
    return user.id();
  }
}

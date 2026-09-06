package com.butingbe.domain.reward.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.dto.response.PointLedgerPageResDto;
import com.butingbe.domain.reward.dto.response.UserRewardsResDto;
import com.butingbe.domain.reward.service.RewardQueryService;
import com.butingbe.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 내 보상 조회(배지·포인트 잔액·원장). 로그인 필요. */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserRewardController {

  private final RewardQueryService rewardQueryService;
  private final com.butingbe.domain.reward.service.UserCouponService userCouponService;

  @GetMapping("/rewards")
  public ResponseEntity<ApiResponse<UserRewardsResDto>> myRewards(
      @AuthenticationPrincipal AuthenticatedUser user) {
    return ResponseEntity.ok(ApiResponse.success("보상 요약 조회", rewardQueryService.myRewards(user)));
  }

  @GetMapping("/point-ledger")
  public ResponseEntity<ApiResponse<PointLedgerPageResDto>> pointLedger(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer size) {
    return ResponseEntity.ok(
        ApiResponse.success("포인트 원장 조회", rewardQueryService.pointLedger(user, cursor, size)));
  }

  @GetMapping("/coupons")
  public ResponseEntity<
          ApiResponse<java.util.List<com.butingbe.domain.reward.dto.response.UserCouponResDto>>>
      myCoupons(
          @AuthenticationPrincipal AuthenticatedUser user,
          @RequestParam(required = false) com.butingbe.domain.reward.entity.CouponStatus status) {
    return ResponseEntity.ok(
        ApiResponse.success("쿠폰함 조회", userCouponService.getMyCoupons(user, status)));
  }

  @org.springframework.web.bind.annotation.PostMapping("/coupons/{couponId}/use")
  public ResponseEntity<ApiResponse<com.butingbe.domain.reward.dto.response.UserCouponResDto>>
      useCoupon(
          @AuthenticationPrincipal AuthenticatedUser user,
          @org.springframework.web.bind.annotation.PathVariable java.util.UUID couponId) {
    return ResponseEntity.ok(
        ApiResponse.success("쿠폰 사용 완료", userCouponService.useCoupon(user, couponId)));
  }
}

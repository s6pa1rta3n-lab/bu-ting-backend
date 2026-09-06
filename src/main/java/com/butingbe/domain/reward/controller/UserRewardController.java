package com.butingbe.domain.reward.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.dto.response.PointLedgerPageResDto;
import com.butingbe.domain.reward.dto.response.UserRewardsResDto;
import com.butingbe.domain.reward.service.UserRewardService;
import com.butingbe.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 유저 보유 보상 및 포인트 원장 조회 컨트롤러. */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserRewardController {

  private final UserRewardService userRewardService;

  /**
   * 유저의 보유 포인트 잔액, 6대 권역 배지 월, 보유 쿠폰 목록을 조회한다.
   *
   * @param user 인증된 유저
   * @return 유저 보유 보상 요약 응답
   */
  @GetMapping("/rewards")
  public ResponseEntity<ApiResponse<UserRewardsResDto>> getMyRewards(
      @AuthenticationPrincipal AuthenticatedUser user) {
    UserRewardsResDto response = userRewardService.getMyRewards(user);
    return ResponseEntity.ok(ApiResponse.success("내 보상 현황 조회", response));
  }

  /**
   * 유저의 포인트 원장 변동 내역을 커서 기반 페이징으로 조회한다.
   *
   * @param user 인증된 유저
   * @param cursor 커서 문자열
   * @param size 페이지 크기 (기본값: 20)
   * @return 포인트 원장 내역 페이징 응답
   */
  @GetMapping("/point-ledger")
  public ResponseEntity<ApiResponse<PointLedgerPageResDto>> getPointLedger(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") Integer size) {
    PointLedgerPageResDto response = userRewardService.getPointLedger(user, cursor, size);
    return ResponseEntity.ok(ApiResponse.success("포인트 원장 내역 조회", response));
  }
}

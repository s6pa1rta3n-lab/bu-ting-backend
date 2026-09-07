package com.butingbe.domain.reward.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.dto.request.AdminPayoutReleaseHoldReqDto;
import com.butingbe.domain.reward.dto.response.AdminPayoutItemResDto;
import com.butingbe.domain.reward.service.RewardPayoutHoldService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 보상 지급 관리 컨트롤러. */
@RestController
@RequestMapping("/admin/reward-payouts")
@RequiredArgsConstructor
public class AdminRewardPayoutController {

  private final RewardPayoutHoldService rewardPayoutHoldService;

  /**
   * 보류된 보상 지급 건의 보류 상태를 해제한다.
   *
   * @param user 요청 관리자
   * @param payoutId 지급 건 식별자
   * @param request 보류 해제 요청
   * @return 보류 해제된 지급 정보
   */
  @PostMapping("/{payoutId}/release-hold")
  public ResponseEntity<ApiResponse<AdminPayoutItemResDto>> releaseHold(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID payoutId,
      @RequestBody(required = false) @Valid AdminPayoutReleaseHoldReqDto request) {
    AdminPayoutReleaseHoldReqDto body =
        request != null ? request : new AdminPayoutReleaseHoldReqDto(null, null);
    AdminPayoutItemResDto response = rewardPayoutHoldService.releaseHold(user, payoutId, body);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 보류 해제 성공", response));
  }
}

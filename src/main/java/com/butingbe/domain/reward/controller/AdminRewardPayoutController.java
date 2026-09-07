package com.butingbe.domain.reward.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.auth.security.OperatorAuthorization;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutBulkConfirmReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutBulkScheduleReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutHoldReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkInfoCollectedReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkMailSentReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkSentReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutPatchReqDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutDetailResDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutPageResDto;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardPayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import com.butingbe.domain.reward.service.AdminRewardPayoutService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin controller for reward payout operations and settlement management. */
@RestController
@RequestMapping("/admin/reward-payouts")
@RequiredArgsConstructor
public class AdminRewardPayoutController {

  private final AdminRewardPayoutService adminRewardPayoutService;
  private final OperatorAuthorization operatorAuthorization;

  /** Retrieves paginated reward payouts matching query filters. */
  @GetMapping
  public ResponseEntity<ApiResponse<RewardPayoutPageResDto>> list(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) UUID roundId,
      @RequestParam(required = false) UUID eventId,
      @RequestParam(required = false) GrantReason rewardReason,
      @RequestParam(required = false) RewardPayoutStatus status,
      @RequestParam(required = false) RewardPayoutHoldStatus holdStatus,
      @RequestParam(required = false) OffsetDateTime scheduledFrom,
      @RequestParam(required = false) OffsetDateTime scheduledTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    operatorAuthorization.requireOperator(user);
    RewardPayoutPageResDto result =
        adminRewardPayoutService.list(
            roundId,
            eventId,
            rewardReason,
            status,
            holdStatus,
            scheduledFrom,
            scheduledTo,
            page,
            size);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 목록 조회", result));
  }

  /** Retrieves detailed information and history timeline for a single payout. */
  @GetMapping("/{payoutId}")
  public ResponseEntity<ApiResponse<RewardPayoutDetailResDto>> detail(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID payoutId) {
    operatorAuthorization.requireOperator(user);
    RewardPayoutDetailResDto result = adminRewardPayoutService.detail(payoutId);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 상세 조회", result));
  }

  /** Updates reward details or schedule for an unconfirmed payout item. */
  @PatchMapping("/{payoutId}")
  public ResponseEntity<ApiResponse<RewardPayoutDetailResDto>> patch(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID payoutId,
      @RequestBody AdminRewardPayoutPatchReqDto request) {
    operatorAuthorization.requireOperator(user);
    RewardPayoutDetailResDto result = adminRewardPayoutService.patch(user.id(), payoutId, request);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 항목 수정", result));
  }

  /** Confirms a list of reward payouts with all-or-nothing validation. */
  @PostMapping("/bulk-confirm")
  public ResponseEntity<ApiResponse<List<UUID>>> bulkConfirm(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid AdminRewardPayoutBulkConfirmReqDto request) {
    operatorAuthorization.requireOperator(user);
    List<UUID> confirmedIds = adminRewardPayoutService.bulkConfirm(user.id(), request);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 일괄 확정", confirmedIds));
  }

  /**
   * Updates scheduled processing dates for multiple reward payouts with all-or-nothing validation.
   */
  @PostMapping("/bulk-schedule")
  public ResponseEntity<ApiResponse<List<UUID>>> bulkSchedule(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid AdminRewardPayoutBulkScheduleReqDto request) {
    operatorAuthorization.requireOperator(user);
    List<UUID> scheduledIds = adminRewardPayoutService.bulkSchedule(user.id(), request);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 일정 일괄 변경", scheduledIds));
  }

  /** Records notification email dispatch for TOP_LIKE reward payouts. */
  @PostMapping("/mark-mail-sent")
  public ResponseEntity<ApiResponse<List<UUID>>> markMailSent(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid AdminRewardPayoutMarkMailSentReqDto request) {
    operatorAuthorization.requireOperator(user);
    List<UUID> result = adminRewardPayoutService.markMailSent(user.id(), request);
    return ResponseEntity.ok(ApiResponse.success("안내 메일 발송 기록 완료", result));
  }

  /** Records winner shipping details collection for physical or voucher prizes. */
  @PostMapping("/mark-info-collected")
  public ResponseEntity<ApiResponse<List<UUID>>> markInfoCollected(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid AdminRewardPayoutMarkInfoCollectedReqDto request) {
    operatorAuthorization.requireOperator(user);
    List<UUID> result = adminRewardPayoutService.markInfoCollected(user.id(), request);
    return ResponseEntity.ok(ApiResponse.success("배송 정보 수집 기록 완료", result));
  }

  /** Records shipping and delivery fulfillment with reference tracking. */
  @PostMapping("/mark-sent")
  public ResponseEntity<ApiResponse<List<UUID>>> markSent(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid AdminRewardPayoutMarkSentReqDto request) {
    operatorAuthorization.requireOperator(user);
    List<UUID> result = adminRewardPayoutService.markSent(user.id(), request);
    return ResponseEntity.ok(ApiResponse.success("발송 완료 기록 및 원장 반영", result));
  }

  /** Retries execution for a failed payout without duplicate grants. */
  @PostMapping("/{payoutId}/retry")
  public ResponseEntity<ApiResponse<RewardPayoutDetailResDto>> retry(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID payoutId) {
    operatorAuthorization.requireOperator(user);
    RewardPayoutDetailResDto result = adminRewardPayoutService.retry(user.id(), payoutId);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 재시도 완료", result));
  }

  /** Puts a reward payout on hold. */
  @PostMapping("/{payoutId}/hold")
  public ResponseEntity<ApiResponse<RewardPayoutDetailResDto>> hold(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID payoutId,
      @RequestBody(required = false) AdminRewardPayoutHoldReqDto request) {
    operatorAuthorization.requireOperator(user);
    String note = request != null ? request.note() : null;
    RewardPayoutDetailResDto result = adminRewardPayoutService.hold(user.id(), payoutId, note);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 보류 설정", result));
  }

  /** Releases a report hold on a reward payout. */
  @PostMapping("/{payoutId}/release-hold")
  public ResponseEntity<ApiResponse<RewardPayoutDetailResDto>> releaseHold(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID payoutId,
      @RequestBody(required = false) AdminRewardPayoutHoldReqDto request) {
    operatorAuthorization.requireOperator(user);
    String note = request != null ? request.note() : null;
    RewardPayoutDetailResDto result =
        adminRewardPayoutService.releaseHold(user.id(), payoutId, note);
    return ResponseEntity.ok(ApiResponse.success("보상 지급 보류 해제", result));
  }
}

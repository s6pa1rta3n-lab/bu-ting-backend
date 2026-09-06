package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.auth.security.OperatorAuthorization;
import com.butingbe.domain.zoneevent.dto.request.ReviewRejectReqDto;
import com.butingbe.domain.zoneevent.dto.response.ReviewQueuePageResDto;
import com.butingbe.domain.zoneevent.service.AdminZoneEventReviewService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin and operator controller for inspection, manual approval, rejection, and revocation. */
@RestController
@RequestMapping("/admin/zone-events/reviews")
@RequiredArgsConstructor
public class AdminZoneEventReviewController {

  private final AdminZoneEventReviewService reviewService;
  private final OperatorAuthorization operatorAuthorization;

  @GetMapping("/queue")
  public ResponseEntity<ApiResponse<ReviewQueuePageResDto>> getQueue(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    operatorAuthorization.requireOperator(user);
    return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewQueue(page, size)));
  }

  @PostMapping("/{participationId}/approve")
  public ResponseEntity<ApiResponse<Void>> approve(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID participationId) {
    operatorAuthorization.requireOperator(user);
    reviewService.approve(user.getUserId(), participationId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/{participationId}/reject")
  public ResponseEntity<ApiResponse<Void>> reject(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID participationId,
      @Valid @RequestBody ReviewRejectReqDto request) {
    operatorAuthorization.requireOperator(user);
    reviewService.reject(user.getUserId(), participationId, request.reason());
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/{participationId}/revoke")
  public ResponseEntity<ApiResponse<Void>> revoke(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID participationId,
      @RequestParam(defaultValue = "Admin revocation") String reason) {
    operatorAuthorization.requireOperator(user);
    reviewService.revoke(user.getUserId(), participationId, reason);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/{participationId}/unhide")
  public ResponseEntity<ApiResponse<Void>> unhide(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID participationId) {
    operatorAuthorization.requireOperator(user);
    reviewService.unhide(user.getUserId(), participationId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}

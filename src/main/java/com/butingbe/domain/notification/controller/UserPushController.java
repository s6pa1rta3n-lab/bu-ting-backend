package com.butingbe.domain.notification.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.notification.dto.request.DeviceTokenRegisterReqDto;
import com.butingbe.domain.notification.dto.request.NotificationSettingReqDto;
import com.butingbe.domain.notification.dto.response.NotificationSettingResDto;
import com.butingbe.domain.notification.dto.response.ZoneSubscriptionResDto;
import com.butingbe.domain.notification.service.NotificationService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller for user device token registration, push preferences, and zone subscriptions. */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserPushController {

  private final NotificationService notificationService;

  @PostMapping("/device-token")
  public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody DeviceTokenRegisterReqDto request) {
    notificationService.registerDeviceToken(
        user.getUserId(), request.fcmToken(), request.deviceType());
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @DeleteMapping("/device-token")
  public ResponseEntity<ApiResponse<Void>> unregisterDeviceToken(
      @AuthenticationPrincipal AuthenticatedUser user, @RequestParam String fcmToken) {
    notificationService.unregisterDeviceToken(user.getUserId(), fcmToken);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @GetMapping("/notification-settings")
  public ResponseEntity<ApiResponse<NotificationSettingResDto>> getNotificationSettings(
      @AuthenticationPrincipal AuthenticatedUser user) {
    return ResponseEntity.ok(
        ApiResponse.success(notificationService.getNotificationSettings(user.getUserId())));
  }

  @PatchMapping("/notification-settings")
  public ResponseEntity<ApiResponse<NotificationSettingResDto>> updateNotificationSettings(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody NotificationSettingReqDto request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            notificationService.updateNotificationSettings(
                user.getUserId(),
                request.pushEnabled(),
                request.zoneEventEnabled(),
                request.settlementEnabled())));
  }

  @GetMapping("/zone-subscriptions")
  public ResponseEntity<ApiResponse<List<ZoneSubscriptionResDto>>> getZoneSubscriptions(
      @AuthenticationPrincipal AuthenticatedUser user) {
    return ResponseEntity.ok(
        ApiResponse.success(notificationService.getUserSubscriptions(user.getUserId())));
  }

  @PostMapping("/zone-subscriptions/{zoneId}")
  public ResponseEntity<ApiResponse<Void>> subscribeZone(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable String zoneId) {
    notificationService.subscribeZone(user.getUserId(), zoneId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @DeleteMapping("/zone-subscriptions/{zoneId}")
  public ResponseEntity<ApiResponse<Void>> unsubscribeZone(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable String zoneId) {
    notificationService.unsubscribeZone(user.getUserId(), zoneId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}

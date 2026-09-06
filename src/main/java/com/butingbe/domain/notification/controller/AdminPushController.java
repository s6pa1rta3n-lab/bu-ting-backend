package com.butingbe.domain.notification.controller;

import com.butingbe.domain.notification.dto.request.AdminPushSendReqDto;
import com.butingbe.domain.notification.service.NotificationService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin controller for manual push notification dispatch. */
@RestController
@RequestMapping("/admin/notifications/push")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@RequiredArgsConstructor
public class AdminPushController {

  private final NotificationService notificationService;

  @PostMapping("/send")
  public ResponseEntity<ApiResponse<Void>> sendPush(
      @Valid @RequestBody AdminPushSendReqDto request) {
    if (request.targetUserId() != null) {
      notificationService.sendToUser(
          request.targetUserId(), request.title(), request.body(), "ADMIN");
    } else if (request.targetZoneId() != null) {
      notificationService.sendToZoneSubscribers(
          request.targetZoneId(), request.title(), request.body());
    } else if (request.topic() != null) {
      notificationService.sendTopic(request.topic(), request.title(), request.body());
    }
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}

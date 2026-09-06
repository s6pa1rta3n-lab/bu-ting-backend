package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.response.UserTitlesSummaryResDto;
import com.butingbe.domain.zoneevent.dto.response.UserZoneTitleResDto;
import com.butingbe.domain.zoneevent.service.ZoneTitleService;
import com.butingbe.global.common.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller managing user zone title collection and equipping. */
@RestController
@RequestMapping("/users/me/titles")
@RequiredArgsConstructor
public class UserZoneTitleController {

  private final ZoneTitleService zoneTitleService;

  @GetMapping
  public ResponseEntity<ApiResponse<UserTitlesSummaryResDto>> getMyTitles(
      @AuthenticationPrincipal AuthenticatedUser user) {
    return ResponseEntity.ok(
        ApiResponse.success(zoneTitleService.getUserTitlesSummary(user.getUserId())));
  }

  @PatchMapping("/{userTitleId}/equip")
  public ResponseEntity<ApiResponse<UserZoneTitleResDto>> equipTitle(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID userTitleId) {
    return ResponseEntity.ok(
        ApiResponse.success(zoneTitleService.equipTitle(user.getUserId(), userTitleId)));
  }

  @PatchMapping("/unequip")
  public ResponseEntity<ApiResponse<Void>> unequipTitle(
      @AuthenticationPrincipal AuthenticatedUser user) {
    zoneTitleService.unequipTitle(user.getUserId());
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}

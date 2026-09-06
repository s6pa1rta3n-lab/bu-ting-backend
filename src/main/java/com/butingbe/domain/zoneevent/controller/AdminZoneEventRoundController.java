package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.request.RainTargetReplaceReqDto;
import com.butingbe.domain.zoneevent.dto.request.RoundCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.SlotReplaceReqDto;
import com.butingbe.domain.zoneevent.dto.response.AutoAssignRecommendationResDto;
import com.butingbe.domain.zoneevent.dto.response.RoundResDto;
import com.butingbe.domain.zoneevent.dto.response.SlotResDto;
import com.butingbe.domain.zoneevent.service.AdminZoneEventRoundService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin controller for round management, slot replacement, and rain target swapping. */
@RestController
@RequestMapping("/admin/zone-events/rounds")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@RequiredArgsConstructor
public class AdminZoneEventRoundController {

  private final AdminZoneEventRoundService roundService;

  @PostMapping
  public ResponseEntity<ApiResponse<RoundResDto>> createRound(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody RoundCreateReqDto request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(roundService.createRound(request, user.getUserId())));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<Page<RoundResDto>>> listRounds(
      @PageableDefault(sort = "startsAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.success(roundService.listRounds(pageable)));
  }

  @GetMapping("/{roundId}")
  public ResponseEntity<ApiResponse<RoundResDto>> getRound(@PathVariable UUID roundId) {
    return ResponseEntity.ok(ApiResponse.success(roundService.getRound(roundId)));
  }

  @PatchMapping("/{roundId}/slots/{slotId}/replace")
  public ResponseEntity<ApiResponse<SlotResDto>> replaceSlot(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID roundId,
      @PathVariable UUID slotId,
      @Valid @RequestBody SlotReplaceReqDto request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            roundService.replaceSlot(roundId, slotId, request.newEventId(), user.getUserId())));
  }

  @PostMapping("/{roundId}/rain-target/replace")
  public ResponseEntity<ApiResponse<Void>> replaceRainTarget(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID roundId,
      @Valid @RequestBody RainTargetReplaceReqDto request) {
    roundService.replaceRainTarget(roundId, request, user.getUserId());
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @GetMapping("/recommendations/auto-assign")
  public ResponseEntity<ApiResponse<AutoAssignRecommendationResDto>> recommendAutoAssign() {
    return ResponseEntity.ok(ApiResponse.success(roundService.recommendAutoAssign()));
  }
}

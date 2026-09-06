package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.request.CommentCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.CommentUpdateReqDto;
import com.butingbe.domain.zoneevent.dto.request.ReportCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.VisibilityUpdateReqDto;
import com.butingbe.domain.zoneevent.dto.response.CommentResDto;
import com.butingbe.domain.zoneevent.dto.response.ReportResDto;
import com.butingbe.domain.zoneevent.service.ZoneEventSocialService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller handling social interactions: likes, comments, abuse reports, and visibility. */
@RestController
@RequestMapping("/zone-events")
@RequiredArgsConstructor
public class ZoneEventSocialController {

  private final ZoneEventSocialService socialService;

  @PostMapping("/participations/{participationId}/likes")
  public ResponseEntity<ApiResponse<Void>> like(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID participationId) {
    socialService.likeParticipation(user.getUserId(), participationId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @DeleteMapping("/participations/{participationId}/likes")
  public ResponseEntity<ApiResponse<Void>> unlike(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID participationId) {
    socialService.unlikeParticipation(user.getUserId(), participationId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/participations/{participationId}/comments")
  public ResponseEntity<ApiResponse<CommentResDto>> addComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID participationId,
      @Valid @RequestBody CommentCreateReqDto request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                socialService.addComment(user.getUserId(), participationId, request.content())));
  }

  @GetMapping("/participations/{participationId}/comments")
  public ResponseEntity<ApiResponse<List<CommentResDto>>> getComments(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID participationId) {
    UUID userId = user != null ? user.getUserId() : null;
    return ResponseEntity.ok(
        ApiResponse.success(socialService.getComments(userId, participationId)));
  }

  @PatchMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<CommentResDto>> updateComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID commentId,
      @Valid @RequestBody CommentUpdateReqDto request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            socialService.updateComment(
                user.getUserId(), commentId, request.content(), user.getRole())));
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<Void>> deleteComment(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID commentId) {
    socialService.deleteComment(user.getUserId(), commentId, user.getRole());
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/participations/{participationId}/reports")
  public ResponseEntity<ApiResponse<ReportResDto>> report(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID participationId,
      @Valid @RequestBody ReportCreateReqDto request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                socialService.reportParticipation(
                    user.getUserId(),
                    participationId,
                    request.reasonCode(),
                    request.reasonDetail())));
  }

  @PatchMapping("/participations/{participationId}/visibility")
  public ResponseEntity<ApiResponse<Void>> updateVisibility(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID participationId,
      @Valid @RequestBody VisibilityUpdateReqDto request) {
    socialService.updateVisibility(
        user.getUserId(), participationId, request.visibility(), user.getRole());
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}

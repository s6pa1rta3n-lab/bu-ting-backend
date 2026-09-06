package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventUpdateReqDto;
import com.butingbe.domain.zoneevent.dto.response.AdminZoneEventPageResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventDetailResDto;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.service.AdminZoneEventService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

/** 관리자 구역 이벤트 운영 컨트롤러. */
@RestController
@RequestMapping("/admin/zone-events")
@RequiredArgsConstructor
public class AdminZoneEventController {

  private final AdminZoneEventService adminZoneEventService;

  /**
   * 신규 구역 이벤트를 생성한다.
   *
   * @param user 인증된 관리자 유저
   * @param request 생성 요청 DTO
   * @return 생성된 이벤트 상세 응답
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ZoneEventDetailResDto>> createEvent(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid AdminZoneEventCreateReqDto request) {
    ZoneEventDetailResDto response = adminZoneEventService.createEvent(user, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("구역 이벤트 생성 성공", response));
  }

  /**
   * 구역 이벤트 목록을 조회한다.
   *
   * @param user 인증된 관리자 유저
   * @param zone 구역 필터
   * @param status 상태 필터
   * @param from 시작 일시 필터
   * @param to 종료 일시 필터
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 이벤트 요약 목록 페이징 응답
   */
  @GetMapping
  public ResponseEntity<ApiResponse<AdminZoneEventPageResDto>> getEvents(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) String zone,
      @RequestParam(required = false) ZoneEventStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    AdminZoneEventPageResDto response =
        adminZoneEventService.getEvents(user, zone, status, from, to, page, size);
    return ResponseEntity.ok(ApiResponse.success("구역 이벤트 목록 조회 성공", response));
  }

  /**
   * 구역 이벤트 단건 상세 정보를 조회한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @return 이벤트 상세 응답
   */
  @GetMapping("/{eventId}")
  public ResponseEntity<ApiResponse<ZoneEventDetailResDto>> getEventDetail(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID eventId) {
    ZoneEventDetailResDto response = adminZoneEventService.getEventDetail(user, eventId);
    return ResponseEntity.ok(ApiResponse.success("구역 이벤트 상세 조회 성공", response));
  }

  /**
   * 구역 이벤트를 수정한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @param request 수정 요청 DTO
   * @return 수정된 이벤트 상세 응답
   */
  @PatchMapping("/{eventId}")
  public ResponseEntity<ApiResponse<ZoneEventDetailResDto>> updateEvent(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID eventId,
      @RequestBody @Valid AdminZoneEventUpdateReqDto request) {
    ZoneEventDetailResDto response = adminZoneEventService.updateEvent(user, eventId, request);
    return ResponseEntity.ok(ApiResponse.success("구역 이벤트 수정 성공", response));
  }

  /**
   * 구역 이벤트를 활성화(ACTIVE)한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @return 활성화된 이벤트 상세 응답
   */
  @PostMapping("/{eventId}/activate")
  public ResponseEntity<ApiResponse<ZoneEventDetailResDto>> activateEvent(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID eventId) {
    ZoneEventDetailResDto response = adminZoneEventService.activateEvent(user, eventId);
    return ResponseEntity.ok(ApiResponse.success("구역 이벤트 활성화 성공", response));
  }

  /**
   * 구역 이벤트를 종료(CLOSED)한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @return 종료된 이벤트 상세 응답
   */
  @PostMapping("/{eventId}/close")
  public ResponseEntity<ApiResponse<ZoneEventDetailResDto>> closeEvent(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID eventId) {
    ZoneEventDetailResDto response = adminZoneEventService.closeEvent(user, eventId);
    return ResponseEntity.ok(ApiResponse.success("구역 이벤트 종료 성공", response));
  }

  /**
   * 구역 이벤트를 취소(CANCELLED)하고 열려 있던 참여들을 취소 처리한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @return 취소된 이벤트 상세 응답
   */
  @PostMapping("/{eventId}/cancel")
  public ResponseEntity<ApiResponse<ZoneEventDetailResDto>> cancelEvent(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID eventId) {
    ZoneEventDetailResDto response = adminZoneEventService.cancelEvent(user, eventId);
    return ResponseEntity.ok(ApiResponse.success("구역 이벤트 취소 성공", response));
  }
}

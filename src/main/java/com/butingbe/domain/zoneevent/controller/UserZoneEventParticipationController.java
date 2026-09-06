package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventParticipationPageResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.service.ZoneEventParticipationService;
import com.butingbe.global.common.ApiResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 유저 구역 이벤트 참여 내역 조회 컨트롤러. */
@RestController
@RequestMapping("/users/me/zone-event-participations")
@RequiredArgsConstructor
public class UserZoneEventParticipationController {

  private final ZoneEventParticipationService participationService;

  /**
   * 유저의 구역 이벤트 참여 내역을 커서 기반 페이징으로 조회한다.
   *
   * @param user 인증된 유저
   * @param cursor 커서 문자열
   * @param size 페이지 크기
   * @param zone 구역 필터
   * @param type 이벤트 유형 필터
   * @param status 참여 상태 필터
   * @param from 조회 시작 시각
   * @param to 조회 종료 시각
   * @return 커서 페이징 참여 내역 응답
   */
  @GetMapping
  public ResponseEntity<ApiResponse<ZoneEventParticipationPageResDto>> getMyParticipations(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String zone,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) ParticipationStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    ZoneEventParticipationPageResDto response =
        participationService.getMyParticipations(user, cursor, size, zone, type, status, from, to);
    return ResponseEntity.ok(ApiResponse.success("내 구역 이벤트 참여 내역 조회", response));
  }
}

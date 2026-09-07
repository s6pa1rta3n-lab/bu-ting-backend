package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.request.AdminReportDismissReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminReportUpholdReqDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportDetailResDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportPageResDto;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.service.AdminReportService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 신고 관리자 검수 컨트롤러. */
@RestController
@RequestMapping("/admin/zone-event-reports")
@RequiredArgsConstructor
public class AdminReportController {

  private final AdminReportService adminReportService;

  /**
   * 신고 목록을 필터링 및 페이징하여 조회한다.
   *
   * @param user 요청 관리자
   * @param status 신고 상태
   * @param roundId 회차 식별자
   * @param eventId 이벤트 식별자
   * @param participationId 참여 식별자
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지당 개수
   * @return 신고 목록 페이징 응답
   */
  @GetMapping
  public ResponseEntity<ApiResponse<AdminReportPageResDto>> getReports(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) ReportStatus status,
      @RequestParam(required = false) UUID roundId,
      @RequestParam(required = false) UUID eventId,
      @RequestParam(required = false) UUID participationId,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, Math.min(Math.max(1, size), 100));
    AdminReportPageResDto result =
        adminReportService.getReports(user, status, roundId, eventId, participationId, pageable);
    return ResponseEntity.ok(ApiResponse.success("신고 목록 조회 성공", result));
  }

  /**
   * 신고 상세 정보, 검수 대상 및 관련 지급 건을 조회한다.
   *
   * @param user 요청 관리자
   * @param reportId 신고 식별자
   * @return 신고 상세 응답
   */
  @GetMapping("/{reportId}")
  public ResponseEntity<ApiResponse<AdminReportDetailResDto>> getReportDetail(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID reportId) {
    AdminReportDetailResDto result = adminReportService.getReportDetail(user, reportId);
    return ResponseEntity.ok(ApiResponse.success("신고 상세 조회 성공", result));
  }

  /**
   * 신고를 인정(UPHELD) 처리하고 관련 미지급 보상을 보류한다.
   *
   * @param user 요청 관리자
   * @param reportId 신고 식별자
   * @param request 인정 요청 본문
   * @return 갱신된 신고 상세 정보
   */
  @PostMapping("/{reportId}/uphold")
  public ResponseEntity<ApiResponse<AdminReportDetailResDto>> upholdReport(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID reportId,
      @RequestBody(required = false) @Valid AdminReportUpholdReqDto request) {
    AdminReportUpholdReqDto body =
        request != null ? request : new AdminReportUpholdReqDto(null, null, null);
    AdminReportDetailResDto result = adminReportService.upholdReport(user, reportId, body);
    return ResponseEntity.ok(ApiResponse.success("신고 인정 처리 성공", result));
  }

  /**
   * 신고를 기각(DISMISSED) 처리한다. 기존 지급 보류는 유지된다.
   *
   * @param user 요청 관리자
   * @param reportId 신고 식별자
   * @param request 기각 요청 본문
   * @return 갱신된 신고 상세 정보
   */
  @PostMapping("/{reportId}/dismiss")
  public ResponseEntity<ApiResponse<AdminReportDetailResDto>> dismissReport(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID reportId,
      @RequestBody(required = false) @Valid AdminReportDismissReqDto request) {
    AdminReportDismissReqDto body =
        request != null ? request : new AdminReportDismissReqDto(null, null);
    AdminReportDetailResDto result = adminReportService.dismissReport(user, reportId, body);
    return ResponseEntity.ok(ApiResponse.success("신고 기각 처리 성공", result));
  }
}

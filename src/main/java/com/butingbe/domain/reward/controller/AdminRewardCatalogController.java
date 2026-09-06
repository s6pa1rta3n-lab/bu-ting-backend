package com.butingbe.domain.reward.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.dto.request.RewardCatalogCreateReqDto;
import com.butingbe.domain.reward.dto.request.RewardCatalogUpdateReqDto;
import com.butingbe.domain.reward.dto.response.RewardCatalogResDto;
import com.butingbe.domain.reward.dto.response.RewardGrantPageResDto;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.service.AdminRewardCatalogService;
import com.butingbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

/** 관리자 보상 카탈로그 및 발급 이력 관리 컨트롤러. */
@RestController
@RequestMapping("/admin/reward-catalog")
@RequiredArgsConstructor
public class AdminRewardCatalogController {

  private final AdminRewardCatalogService adminRewardCatalogService;

  /**
   * 보상 카탈로그 목록을 필터링하여 조회한다.
   *
   * @param user 인증된 관리자 유저
   * @param rewardType 보상 구분 필터
   * @param active 활성화 상태 필터
   * @return 보상 카탈로그 목록 응답
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<RewardCatalogResDto>>> getCatalog(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) RewardType rewardType,
      @RequestParam(required = false) Boolean active) {
    List<RewardCatalogResDto> response =
        adminRewardCatalogService.getCatalog(user, rewardType, active);
    return ResponseEntity.ok(ApiResponse.success("보상 카탈로그 목록 조회 성공", response));
  }

  /**
   * 신규 보상 카탈로그 항목을 생성한다.
   *
   * @param user 인증된 관리자 유저
   * @param request 생성 요청 DTO
   * @return 생성된 보상 카탈로그 상세 응답
   */
  @PostMapping
  public ResponseEntity<ApiResponse<RewardCatalogResDto>> createCatalog(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Valid RewardCatalogCreateReqDto request) {
    RewardCatalogResDto response = adminRewardCatalogService.createCatalog(user, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("보상 카탈로그 항목 생성 성공", response));
  }

  /**
   * 보상 카탈로그 항목의 이름, 재고, 월별 한도, 활성 상태를 수정한다.
   *
   * @param user 인증된 관리자 유저
   * @param rewardId 보상 식별자
   * @param request 수정 요청 DTO
   * @return 수정된 보상 카탈로그 상세 응답
   */
  @PatchMapping("/{rewardId}")
  public ResponseEntity<ApiResponse<RewardCatalogResDto>> updateCatalog(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID rewardId,
      @RequestBody @Valid RewardCatalogUpdateReqDto request) {
    RewardCatalogResDto response = adminRewardCatalogService.updateCatalog(user, rewardId, request);
    return ResponseEntity.ok(ApiResponse.success("보상 카탈로그 항목 수정 성공", response));
  }

  /**
   * 특정 보상 항목의 발급 이력을 커서 기반 페이징으로 조회한다.
   *
   * @param user 인증된 관리자 유저
   * @param rewardId 보상 식별자
   * @param cursor 커서 문자열
   * @param size 페이지 크기
   * @return 보상 발급 이력 커서 페이징 응답
   */
  @GetMapping("/{rewardId}/grants")
  public ResponseEntity<ApiResponse<RewardGrantPageResDto>> getGrants(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID rewardId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") Integer size) {
    RewardGrantPageResDto response =
        adminRewardCatalogService.getGrants(user, rewardId, cursor, size);
    return ResponseEntity.ok(ApiResponse.success("보상 발급 이력 조회 성공", response));
  }
}

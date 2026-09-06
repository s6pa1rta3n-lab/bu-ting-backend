package com.butingbe.domain.reward.dto.request;

/**
 * 관리자 보상 카탈로그 수정 요청 DTO.
 *
 * @param name 보상 이름
 * @param stock 재고 수량
 * @param monthlyCap 월별 발급 한도
 * @param active 활성화 여부
 */
public record RewardCatalogUpdateReqDto(
    String name, Integer stock, Integer monthlyCap, Boolean active) {}

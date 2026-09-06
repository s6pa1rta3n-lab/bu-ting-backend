package com.butingbe.domain.reward.dto.request;

import com.butingbe.domain.reward.entity.RewardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 보상 카탈로그 생성 요청 DTO.
 *
 * @param rewardType 보상 구분 (POINT/BADGE/COUPON/GIFTICON)
 * @param code 고유 카탈로그 코드
 * @param name 보상 이름
 * @param pointAmount 포인트 금액
 * @param imageFileKey 이미지 파일 키
 * @param stock 재고 수량
 * @param monthlyCap 월별 발급 한도
 * @param validDays 유효 기간(일)
 * @param active 활성화 여부
 */
public record RewardCatalogCreateReqDto(
    @NotNull RewardType rewardType,
    @NotBlank String code,
    @NotBlank String name,
    Integer pointAmount,
    String imageFileKey,
    Integer stock,
    Integer monthlyCap,
    Integer validDays,
    Boolean active) {}

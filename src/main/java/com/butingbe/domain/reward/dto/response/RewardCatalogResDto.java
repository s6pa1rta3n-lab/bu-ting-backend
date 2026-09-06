package com.butingbe.domain.reward.dto.response;

import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardType;
import java.time.LocalDateTime;

/**
 * 보상 카탈로그 단건 응답 DTO.
 *
 * @param rewardId 보상 식별자
 * @param rewardType 보상 유형 (POINT/BADGE/COUPON/GIFTICON)
 * @param code 고유 카탈로그 코드
 * @param name 보상 명칭
 * @param pointAmount 포인트 금액
 * @param imageFileKey 이미지 파일 키
 * @param imageUrl 이미지 사전 서명 URL
 * @param stock 재고 수량
 * @param monthlyCap 월별 발급 한도
 * @param validDays 유효 기간(일)
 * @param active 활성화 여부
 * @param createdAt 생성 일시
 * @param updatedAt 수정 일시
 */
public record RewardCatalogResDto(
    String rewardId,
    RewardType rewardType,
    String code,
    String name,
    Integer pointAmount,
    String imageFileKey,
    String imageUrl,
    Integer stock,
    Integer monthlyCap,
    Integer validDays,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static RewardCatalogResDto of(RewardCatalog catalog, String imageUrl) {
    return new RewardCatalogResDto(
        catalog.getId().toString(),
        catalog.getRewardType(),
        catalog.getCode(),
        catalog.getName(),
        catalog.getPointAmount(),
        catalog.getImageFileKey(),
        imageUrl,
        catalog.getStock(),
        catalog.getMonthlyCap(),
        catalog.getValidDays(),
        catalog.getActive(),
        catalog.getCreatedAt(),
        catalog.getUpdatedAt());
  }
}

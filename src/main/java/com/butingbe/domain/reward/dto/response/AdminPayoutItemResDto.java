package com.butingbe.domain.reward.dto.response;

import com.butingbe.domain.reward.entity.BaseRewardPayout;
import com.butingbe.domain.reward.entity.PayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 보상 지급 항목 응답 DTO.
 *
 * @param payoutId 지급 건 식별자
 * @param payoutType 지급 종류 (BASE, TOP_LIKE)
 * @param participationId 참여 식별자
 * @param eventId 이벤트 식별자
 * @param rankN 순위 (TOP_LIKE 전용)
 * @param likeCountAtClose 마감 시점 좋아요 수 (TOP_LIKE 전용)
 * @param reward 보상 스냅샷
 * @param status 지급 진행 상태
 * @param holdStatus 지급 보류 상태
 * @param revision 리비전
 * @param createdAt 생성 일시
 * @param updatedAt 수정 일시
 */
public record AdminPayoutItemResDto(
    UUID payoutId,
    String payoutType,
    UUID participationId,
    UUID eventId,
    Integer rankN,
    Long likeCountAtClose,
    RewardSnapshot reward,
    String status,
    PayoutHoldStatus holdStatus,
    Long revision,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  /** 기본 보상 지급 엔티티로부터 DTO를 생성한다. */
  public static AdminPayoutItemResDto from(BaseRewardPayout payout) {
    return new AdminPayoutItemResDto(
        payout.getId(),
        "BASE",
        payout.getParticipationId(),
        null,
        null,
        null,
        payout.getReward(),
        payout.getStatus().name(),
        payout.getHoldStatus(),
        payout.getRevision(),
        payout.getCreatedAt(),
        payout.getUpdatedAt());
  }

  /** 우수 보상 지급 엔티티로부터 DTO를 생성한다. */
  public static AdminPayoutItemResDto from(RewardPayout payout) {
    return new AdminPayoutItemResDto(
        payout.getId(),
        "TOP_LIKE",
        payout.getParticipationId(),
        payout.getEventId(),
        payout.getRankN(),
        payout.getLikeCountAtClose(),
        payout.getReward(),
        payout.getStatus().name(),
        payout.getHoldStatus(),
        payout.getRevision(),
        payout.getCreatedAt(),
        payout.getUpdatedAt());
  }
}

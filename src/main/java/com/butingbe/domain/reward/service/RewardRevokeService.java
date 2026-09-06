package com.butingbe.domain.reward.service;

import com.butingbe.domain.reward.dto.response.ParticipationRevokeResDto;
import com.butingbe.domain.reward.entity.CouponStatus;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.entity.UserCoupon;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.reward.repository.UserBadgeRepository;
import com.butingbe.domain.reward.repository.UserCouponRepository;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참여 무효화 및 보상 회수 서비스 (FR-RWD-08).
 *
 * <p>참여가 REVOKED 처리될 때 BASE 및 TOP_LIKE 사유의 보상을 무효화(revoked_at)한다. 포인트 원장에 음수를 반영하고, 실물 보상(쿠폰)은 미사용
 * 상태인 경우에만 회수한다.
 */
@Service
@RequiredArgsConstructor
public class RewardRevokeService {

  private final ZoneEventParticipationRepository participationRepository;
  private final RewardGrantRepository rewardGrantRepository;
  private final UserCouponRepository userCouponRepository;
  private final UserBadgeRepository userBadgeRepository;
  private final UserPointService userPointService;
  private final com.butingbe.domain.reward.repository.UserPointLedgerRepository
      userPointLedgerRepository;

  /** 참여를 무효화하고 관련 보상을 회수한다. */
  @Transactional
  public ParticipationRevokeResDto revokeParticipation(UUID participationId) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event_participation.not_found"));

    participation.revoke();

    List<RewardGrant> grants =
        rewardGrantRepository.findByParticipationIdAndRevokedAtIsNull(participationId);
    OffsetDateTime now = OffsetDateTime.now();

    int revokedGrantsCount = 0;
    int revokedPointsAmount = 0;
    int revokedCouponsCount = 0;

    for (RewardGrant grant : grants) {
      GrantReason reason = grant.getGrantReason();
      if (reason != GrantReason.BASE && reason != GrantReason.TOP_LIKE) {
        continue;
      }

      RewardType type = grant.getReward().getRewardType();
      if (type == RewardType.COUPON || type == RewardType.GIFTICON) {
        Optional<UserCoupon> optCoupon = userCouponRepository.findByGrantId(grant.getId());
        if (optCoupon.isPresent()) {
          UserCoupon coupon = optCoupon.get();
          if (coupon.getStatus() == CouponStatus.USED) {
            continue;
          }
          userCouponRepository.delete(coupon);
          coupon.getReward().increaseStock();
          grant.revoke(now);
          revokedGrantsCount++;
          revokedCouponsCount++;
        }
      } else if (type == RewardType.POINT) {
        grant.revoke(now);
        int points =
            userPointLedgerRepository
                .findByGrantId(grant.getId())
                .map(com.butingbe.domain.reward.entity.UserPointLedger::getAmount)
                .orElseGet(
                    () ->
                        grant.getReward().getPointAmount() != null
                            ? grant.getReward().getPointAmount()
                            : 0);
        if (points > 0) {
          userPointService.record(grant.getUserId(), -points, "REVOKE", grant.getId());
          revokedPointsAmount += points;
        }
        revokedGrantsCount++;
      } else if (type == RewardType.BADGE) {
        grant.revoke(now);
        userBadgeRepository.deleteByGrantId(grant.getId());
        revokedGrantsCount++;
      }
    }

    return new ParticipationRevokeResDto(
        participationId,
        ParticipationStatus.REVOKED,
        revokedGrantsCount,
        revokedPointsAmount,
        revokedCouponsCount);
  }
}

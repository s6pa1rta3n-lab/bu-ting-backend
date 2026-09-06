package com.butingbe.domain.reward.service;

import com.butingbe.domain.reward.dto.response.SettlementItemStatus;
import com.butingbe.domain.reward.dto.response.TopLikeSettlementItemResDto;
import com.butingbe.domain.reward.dto.response.TopLikeSettlementReportResDto;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.entity.UserBadge;
import com.butingbe.domain.reward.entity.UserCoupon;
import com.butingbe.domain.reward.repository.RewardCatalogRepository;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.reward.repository.UserBadgeRepository;
import com.butingbe.domain.reward.repository.UserCouponRepository;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TOP_LIKE 우수 보상 정산 서비스 (FR-RWD-05, FR-RWD-06).
 *
 * <p>이벤트별 공개 SUCCESS 인증을 좋아요 순(동점 시 완료 시각 순) 상위 N명에게 우수 보상을 지급한다. 회차 단위 락(BR-12)과 멱등성을 보장하며, 실물 보상
 * 재고 및 월 캡 부족 시 스킵 리포트를 생성한다.
 */
@Service
@RequiredArgsConstructor
public class TopLikeSettlementService {

  private static final String POINT_REWARD_CODE = "POINT_BASE";

  private final ZoneEventRepository zoneEventRepository;
  private final ZoneEventParticipationRepository participationRepository;
  private final RewardCatalogRepository rewardCatalogRepository;
  private final RewardGrantRepository rewardGrantRepository;
  private final UserCouponRepository userCouponRepository;
  private final UserBadgeRepository userBadgeRepository;
  private final UserPointService userPointService;
  private final SettlementLockManager lockManager;

  /** 특정 이벤트의 TOP_LIKE 우수 보상을 정산한다. */
  @Transactional
  public TopLikeSettlementReportResDto settleEvent(UUID eventId) {
    return lockManager.executeWithLock(eventId, () -> executeSettleEvent(eventId));
  }

  /** 특정 회차의 모든 이벤트에 대해 TOP_LIKE 우수 보상을 정산한다. */
  @Transactional
  public List<TopLikeSettlementReportResDto> settleRound(UUID roundId) {
    return lockManager.executeWithLock(roundId, () -> executeSettleRound(roundId));
  }

  private List<TopLikeSettlementReportResDto> executeSettleRound(UUID roundId) {
    List<ZoneEvent> events = zoneEventRepository.findByRoundId(roundId);
    List<TopLikeSettlementReportResDto> reports = new ArrayList<>();
    for (ZoneEvent event : events) {
      reports.add(executeSettleEvent(event.getId()));
    }
    return reports;
  }

  private TopLikeSettlementReportResDto executeSettleEvent(UUID eventId) {
    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    RewardSnapshot excellence = event.getExcellenceReward();
    if (excellence == null || excellence.topN() == null || excellence.topN() <= 0) {
      return new TopLikeSettlementReportResDto(eventId, 0, 0, 0, 0, List.of());
    }

    int topN = excellence.topN();
    List<ZoneEventParticipation> candidates =
        participationRepository.findTopCandidates(
            eventId,
            ParticipationStatus.SUCCESS,
            ParticipationVisibility.PUBLIC,
            PageRequest.of(0, topN));

    List<TopLikeSettlementItemResDto> items = new ArrayList<>();
    int grantedCount = 0;
    int skippedStockCount = 0;
    int skippedMonthlyCapCount = 0;

    for (int i = 0; i < candidates.size(); i++) {
      ZoneEventParticipation candidate = candidates.get(i);
      int rank = i + 1;

      TopLikeSettlementItemResDto itemResult = processCandidate(event, candidate, rank, excellence);
      items.add(itemResult);

      if (itemResult.status() == SettlementItemStatus.GRANTED
          || itemResult.status() == SettlementItemStatus.ALREADY_GRANTED) {
        grantedCount++;
      } else if (itemResult.status() == SettlementItemStatus.SKIPPED_OUT_OF_STOCK) {
        skippedStockCount++;
      } else if (itemResult.status() == SettlementItemStatus.SKIPPED_MONTHLY_CAP) {
        skippedMonthlyCapCount++;
      }
    }

    return new TopLikeSettlementReportResDto(
        eventId, candidates.size(), grantedCount, skippedStockCount, skippedMonthlyCapCount, items);
  }

  private TopLikeSettlementItemResDto processCandidate(
      ZoneEvent event, ZoneEventParticipation candidate, int rank, RewardSnapshot excellence) {
    OffsetDateTime now = OffsetDateTime.now();
    UUID participationId = candidate.getId();
    UUID userId = candidate.getUserId();

    if (excellence.points() != null && excellence.points() > 0) {
      grantPoints(event, participationId, userId, excellence.points(), now);
    }

    if (excellence.badgeCode() != null && !excellence.badgeCode().isBlank()) {
      grantBadge(event, participationId, userId, excellence.badgeCode(), now);
    }

    String prizeCode = excellence.prizeRewardCode();
    if (prizeCode == null || prizeCode.isBlank()) {
      return new TopLikeSettlementItemResDto(
          event.getId(),
          participationId,
          userId,
          rank,
          candidate.getLikeCount(),
          null,
          SettlementItemStatus.GRANTED,
          null,
          null);
    }

    Optional<RewardCatalog> optCatalog = rewardCatalogRepository.findByCode(prizeCode);
    if (optCatalog.isEmpty() || !Boolean.TRUE.equals(optCatalog.get().getActive())) {
      return new TopLikeSettlementItemResDto(
          event.getId(),
          participationId,
          userId,
          rank,
          candidate.getLikeCount(),
          prizeCode,
          SettlementItemStatus.SKIPPED_NO_REWARD,
          null,
          null);
    }

    RewardCatalog catalog = optCatalog.get();

    if (rewardGrantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
        participationId, GrantReason.TOP_LIKE, catalog.getId())) {
      Optional<UserCoupon> existingCoupon =
          userCouponRepository.findByUserIdOrderByIssuedAtDesc(userId).stream()
              .filter(c -> c.getReward().getId().equals(catalog.getId()))
              .findFirst();
      return new TopLikeSettlementItemResDto(
          event.getId(),
          participationId,
          userId,
          rank,
          candidate.getLikeCount(),
          prizeCode,
          SettlementItemStatus.ALREADY_GRANTED,
          null,
          existingCoupon.map(UserCoupon::getId).orElse(null));
    }

    if (catalog.getMonthlyCap() != null) {
      OffsetDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
      OffsetDateTime startOfNextMonth = startOfMonth.plusMonths(1);
      long monthlyUsage =
          rewardGrantRepository.countActiveGrantsInMonth(
              catalog.getId(), startOfMonth, startOfNextMonth);
      if (monthlyUsage >= catalog.getMonthlyCap()) {
        return new TopLikeSettlementItemResDto(
            event.getId(),
            participationId,
            userId,
            rank,
            candidate.getLikeCount(),
            prizeCode,
            SettlementItemStatus.SKIPPED_MONTHLY_CAP,
            null,
            null);
      }
    }

    if (catalog.getStock() != null && catalog.getStock() <= 0) {
      return new TopLikeSettlementItemResDto(
          event.getId(),
          participationId,
          userId,
          rank,
          candidate.getLikeCount(),
          prizeCode,
          SettlementItemStatus.SKIPPED_OUT_OF_STOCK,
          null,
          null);
    }

    catalog.decreaseStock();

    RewardGrant grant =
        rewardGrantRepository.save(
            RewardGrant.builder()
                .userId(userId)
                .reward(catalog)
                .participationId(participationId)
                .eventId(event.getId())
                .roundId(event.getRoundId())
                .grantReason(GrantReason.TOP_LIKE)
                .grantedAt(now)
                .build());

    UUID couponId = null;
    if (catalog.getRewardType() == RewardType.COUPON
        || catalog.getRewardType() == RewardType.GIFTICON) {
      String couponCode = generateCouponCode();
      OffsetDateTime expiresAt =
          catalog.getValidDays() != null ? now.plusDays(catalog.getValidDays()) : null;
      UserCoupon coupon =
          userCouponRepository.save(
              UserCoupon.builder()
                  .userId(userId)
                  .reward(catalog)
                  .grantId(grant.getId())
                  .couponCode(couponCode)
                  .issuedAt(now)
                  .expiresAt(expiresAt)
                  .build());
      couponId = coupon.getId();
    }

    return new TopLikeSettlementItemResDto(
        event.getId(),
        participationId,
        userId,
        rank,
        candidate.getLikeCount(),
        prizeCode,
        SettlementItemStatus.GRANTED,
        grant.getId(),
        couponId);
  }

  private void grantPoints(
      ZoneEvent event, UUID participationId, UUID userId, int points, OffsetDateTime now) {
    rewardCatalogRepository
        .findByCode(POINT_REWARD_CODE)
        .ifPresent(
            pointCatalog -> {
              if (!rewardGrantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
                  participationId, GrantReason.TOP_LIKE, pointCatalog.getId())) {
                RewardGrant grant =
                    rewardGrantRepository.save(
                        RewardGrant.builder()
                            .userId(userId)
                            .reward(pointCatalog)
                            .participationId(participationId)
                            .eventId(event.getId())
                            .roundId(event.getRoundId())
                            .grantReason(GrantReason.TOP_LIKE)
                            .grantedAt(now)
                            .build());
                userPointService.record(userId, points, GrantReason.TOP_LIKE.name(), grant.getId());
              }
            });
  }

  private void grantBadge(
      ZoneEvent event, UUID participationId, UUID userId, String badgeCode, OffsetDateTime now) {
    rewardCatalogRepository
        .findByCode(badgeCode)
        .ifPresent(
            badgeCatalog -> {
              if (!rewardGrantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
                  participationId, GrantReason.TOP_LIKE, badgeCatalog.getId())) {
                RewardGrant grant =
                    rewardGrantRepository.save(
                        RewardGrant.builder()
                            .userId(userId)
                            .reward(badgeCatalog)
                            .participationId(participationId)
                            .eventId(event.getId())
                            .roundId(event.getRoundId())
                            .grantReason(GrantReason.TOP_LIKE)
                            .grantedAt(now)
                            .build());
                if (!userBadgeRepository.existsByUserIdAndReward_Id(userId, badgeCatalog.getId())) {
                  userBadgeRepository.save(
                      UserBadge.builder()
                          .userId(userId)
                          .reward(badgeCatalog)
                          .grantId(grant.getId())
                          .build());
                }
              }
            });
  }

  private String generateCouponCode() {
    return "CPN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
  }
}

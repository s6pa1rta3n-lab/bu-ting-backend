package com.butingbe.domain.reward.service;

import com.butingbe.domain.reward.dto.request.AdminRewardPayoutBulkConfirmReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutBulkScheduleReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkInfoCollectedReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkMailSentReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkSentReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutPatchReqDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutDetailResDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutPageResDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutSummaryResDto;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.entity.RewardPayoutAction;
import com.butingbe.domain.reward.entity.RewardPayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import com.butingbe.domain.reward.entity.UserCoupon;
import com.butingbe.domain.reward.exception.RewardPayoutConflictException;
import com.butingbe.domain.reward.repository.RewardCatalogRepository;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.reward.repository.RewardPayoutHistoryRepository;
import com.butingbe.domain.reward.repository.RewardPayoutRepository;
import com.butingbe.domain.reward.repository.UserCouponRepository;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service managing reward payout lifecycle, validation, confirmation, and settlement execution. */
@Service
@RequiredArgsConstructor
public class AdminRewardPayoutService {

  private static final Set<RewardPayoutStatus> CONFIRMED_OR_LATER_STATUSES =
      EnumSet.of(
          RewardPayoutStatus.CONFIRMED,
          RewardPayoutStatus.PAID,
          RewardPayoutStatus.MAIL_SENT,
          RewardPayoutStatus.INFO_COLLECTED,
          RewardPayoutStatus.SENT);

  private final RewardPayoutRepository rewardPayoutRepository;
  private final RewardPayoutHistoryRepository rewardPayoutHistoryRepository;
  private final RewardCatalogRepository rewardCatalogRepository;
  private final RewardGrantRepository rewardGrantRepository;
  private final UserCouponRepository userCouponRepository;
  private final ZoneEventParticipationRepository participationRepository;
  private final RewardService rewardService;

  /** Retrieves paginated reward payouts matching query filters. */
  @Transactional(readOnly = true)
  public RewardPayoutPageResDto list(
      UUID roundId,
      UUID eventId,
      GrantReason rewardReason,
      RewardPayoutStatus status,
      RewardPayoutHoldStatus holdStatus,
      OffsetDateTime scheduledFrom,
      OffsetDateTime scheduledTo,
      int page,
      int size) {
    Specification<RewardPayout> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (roundId != null) {
            predicates.add(cb.equal(root.get("roundId"), roundId));
          }
          if (eventId != null) {
            predicates.add(cb.equal(root.get("eventId"), eventId));
          }
          if (rewardReason != null) {
            predicates.add(cb.equal(root.get("rewardReason"), rewardReason));
          }
          if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
          }
          if (holdStatus != null) {
            predicates.add(cb.equal(root.get("holdStatus"), holdStatus));
          }
          if (scheduledFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), scheduledFrom));
          }
          if (scheduledTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("scheduledAt"), scheduledTo));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<RewardPayoutSummaryResDto> resultPage =
        rewardPayoutRepository.findAll(spec, pageRequest).map(RewardPayoutSummaryResDto::from);

    return RewardPayoutPageResDto.from(resultPage);
  }

  /** Retrieves full details and timeline history for a specific payout ID. */
  @Transactional(readOnly = true)
  public RewardPayoutDetailResDto detail(UUID payoutId) {
    RewardPayout payout =
        rewardPayoutRepository
            .findById(payoutId)
            .orElseThrow(() -> new ResourceNotFoundException("error.reward_payout.not_found"));
    return RewardPayoutDetailResDto.from(payout);
  }

  /** Updates reward configuration or notes for an unconfirmed payout item. */
  @Transactional
  public RewardPayoutDetailResDto patch(
      UUID actorId, UUID payoutId, AdminRewardPayoutPatchReqDto req) {
    RewardPayout payout =
        rewardPayoutRepository
            .findById(payoutId)
            .orElseThrow(() -> new ResourceNotFoundException("error.reward_payout.not_found"));

    if (req.expectedRevision() != null
        && !Objects.equals(payout.getRevision(), req.expectedRevision())) {
      throw new ConflictException("error.reward_payout.conflict");
    }

    boolean modifiesReward =
        req.rewardCatalogId() != null
            || req.rewardCode() != null
            || req.rewardName() != null
            || req.points() != null
            || req.badgeCode() != null;

    if (CONFIRMED_OR_LATER_STATUSES.contains(payout.getStatus()) && modifiesReward) {
      throw new ConflictException("error.reward_payout.cannot_change_reward_after_confirm");
    }

    RewardCatalog newCatalog = null;
    if (req.rewardCatalogId() != null) {
      newCatalog =
          rewardCatalogRepository
              .findById(req.rewardCatalogId())
              .orElseThrow(() -> new ResourceNotFoundException("error.reward.catalog_not_found"));
    } else if (req.rewardCode() != null) {
      newCatalog =
          rewardCatalogRepository
              .findByCode(req.rewardCode())
              .orElseThrow(() -> new ResourceNotFoundException("error.reward.catalog_not_found"));
    }

    RewardPayoutStatus fromStatus = payout.getStatus();
    payout.updateRewardConfig(
        newCatalog, req.points(), req.badgeCode(), req.rewardName(), req.scheduledAt(), req.note());
    payout.incrementRevision();
    payout.addHistory(
        RewardPayoutAction.UPDATE_CONFIG, fromStatus, payout.getStatus(), actorId, req.note());

    return RewardPayoutDetailResDto.from(payout);
  }

  /**
   * Confirms multiple payouts all-or-nothing with concurrency checks and immediate base reward
   * settlement.
   */
  @Transactional
  public List<UUID> bulkConfirm(UUID actorId, AdminRewardPayoutBulkConfirmReqDto req) {
    List<UUID> problematicIds = new ArrayList<>();
    List<RewardPayout> targets = new ArrayList<>();

    for (UUID id : req.payoutIds()) {
      RewardPayout payout = rewardPayoutRepository.findById(id).orElse(null);
      if (payout == null) {
        problematicIds.add(id);
        continue;
      }
      if (req.expectedRevisions() != null && req.expectedRevisions().containsKey(id)) {
        if (!Objects.equals(payout.getRevision(), req.expectedRevisions().get(id))) {
          problematicIds.add(id);
          continue;
        }
      }
      if (payout.getHoldStatus() != RewardPayoutHoldStatus.NONE) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getStatus() != RewardPayoutStatus.PENDING_CONFIRM) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getParticipationId() != null) {
        ZoneEventParticipation participation =
            participationRepository.findById(payout.getParticipationId()).orElse(null);
        if (participation == null || participation.getStatus() != ParticipationStatus.SUCCESS) {
          problematicIds.add(id);
          continue;
        }
      }
      if (payout.getRewardReason() == GrantReason.TOP_LIKE
          && payout.getRewardCatalog() == null
          && payout.getPrizeName() == null) {
        problematicIds.add(id);
        continue;
      }
      targets.add(payout);
    }

    if (!problematicIds.isEmpty()) {
      throw new RewardPayoutConflictException("error.reward_payout.conflict", problematicIds);
    }

    OffsetDateTime now = OffsetDateTime.now();
    for (RewardPayout payout : targets) {
      RewardPayoutStatus fromStatus = payout.getStatus();
      payout.confirm(actorId);

      if (payout.getRewardReason() == GrantReason.BASE) {
        if (payout.getScheduledAt() == null || !payout.getScheduledAt().isAfter(now)) {
          executeBasePayoutGrant(payout);
          payout.markPaid(now);
          payout.addHistory(
              RewardPayoutAction.EXECUTE_PAY, fromStatus, RewardPayoutStatus.PAID, actorId, null);
        } else {
          payout.addHistory(
              RewardPayoutAction.CONFIRM, fromStatus, RewardPayoutStatus.CONFIRMED, actorId, null);
        }
      } else {
        payout.addHistory(
            RewardPayoutAction.CONFIRM, fromStatus, RewardPayoutStatus.CONFIRMED, actorId, null);
      }

      payout.incrementRevision();
      rewardPayoutRepository.save(payout);
    }

    return req.payoutIds();
  }

  /** Sets scheduled processing date for multiple payouts all-or-nothing. */
  @Transactional
  public List<UUID> bulkSchedule(UUID actorId, AdminRewardPayoutBulkScheduleReqDto req) {
    List<UUID> problematicIds = new ArrayList<>();
    List<RewardPayout> targets = new ArrayList<>();

    for (UUID id : req.payoutIds()) {
      RewardPayout payout = rewardPayoutRepository.findById(id).orElse(null);
      if (payout == null) {
        problematicIds.add(id);
        continue;
      }
      if (req.expectedRevisions() != null && req.expectedRevisions().containsKey(id)) {
        if (!Objects.equals(payout.getRevision(), req.expectedRevisions().get(id))) {
          problematicIds.add(id);
          continue;
        }
      }
      if (payout.getHoldStatus() != RewardPayoutHoldStatus.NONE) {
        problematicIds.add(id);
        continue;
      }
      targets.add(payout);
    }

    if (!problematicIds.isEmpty()) {
      throw new RewardPayoutConflictException("error.reward_payout.conflict", problematicIds);
    }

    for (RewardPayout payout : targets) {
      payout.schedule(req.scheduledAt());
      payout.incrementRevision();
      payout.addHistory(
          RewardPayoutAction.SCHEDULE, payout.getStatus(), payout.getStatus(), actorId, null);
      rewardPayoutRepository.save(payout);
    }

    return req.payoutIds();
  }

  /** Records notification email dispatch for TOP_LIKE reward recipients. */
  @Transactional
  public List<UUID> markMailSent(UUID actorId, AdminRewardPayoutMarkMailSentReqDto req) {
    List<UUID> problematicIds = new ArrayList<>();
    List<RewardPayout> targets = new ArrayList<>();

    for (UUID id : req.payoutIds()) {
      RewardPayout payout = rewardPayoutRepository.findById(id).orElse(null);
      if (payout == null) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getRewardReason() != GrantReason.TOP_LIKE) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getStatus() != RewardPayoutStatus.CONFIRMED) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getHoldStatus() != RewardPayoutHoldStatus.NONE) {
        problematicIds.add(id);
        continue;
      }
      if (req.expectedRevisions() != null && req.expectedRevisions().containsKey(id)) {
        if (!Objects.equals(payout.getRevision(), req.expectedRevisions().get(id))) {
          problematicIds.add(id);
          continue;
        }
      }
      targets.add(payout);
    }

    if (!problematicIds.isEmpty()) {
      throw new RewardPayoutConflictException("error.reward_payout.conflict", problematicIds);
    }

    for (RewardPayout payout : targets) {
      RewardPayoutStatus from = payout.getStatus();
      payout.markMailSent(req.mailedAt(), req.note());
      payout.incrementRevision();
      payout.addHistory(
          RewardPayoutAction.MARK_MAIL_SENT, from, payout.getStatus(), actorId, req.note());
      rewardPayoutRepository.save(payout);
    }

    return req.payoutIds();
  }

  /** Records recipient information receipt for physical or coupon prize delivery. */
  @Transactional
  public List<UUID> markInfoCollected(UUID actorId, AdminRewardPayoutMarkInfoCollectedReqDto req) {
    List<UUID> problematicIds = new ArrayList<>();
    List<RewardPayout> targets = new ArrayList<>();

    for (UUID id : req.payoutIds()) {
      RewardPayout payout = rewardPayoutRepository.findById(id).orElse(null);
      if (payout == null) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getRewardReason() != GrantReason.TOP_LIKE) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getStatus() != RewardPayoutStatus.MAIL_SENT) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getHoldStatus() != RewardPayoutHoldStatus.NONE) {
        problematicIds.add(id);
        continue;
      }
      if (req.expectedRevisions() != null && req.expectedRevisions().containsKey(id)) {
        if (!Objects.equals(payout.getRevision(), req.expectedRevisions().get(id))) {
          problematicIds.add(id);
          continue;
        }
      }
      targets.add(payout);
    }

    if (!problematicIds.isEmpty()) {
      throw new RewardPayoutConflictException("error.reward_payout.conflict", problematicIds);
    }

    for (RewardPayout payout : targets) {
      RewardPayoutStatus from = payout.getStatus();
      payout.markInfoCollected(req.informationCollectedAt(), req.note());
      payout.incrementRevision();
      payout.addHistory(
          RewardPayoutAction.MARK_INFO_COLLECTED, from, payout.getStatus(), actorId, req.note());
      rewardPayoutRepository.save(payout);
    }

    return req.payoutIds();
  }

  /** Records delivery shipment completion and atomically grants prize to ledger. */
  @Transactional
  public List<UUID> markSent(UUID actorId, AdminRewardPayoutMarkSentReqDto req) {
    List<UUID> problematicIds = new ArrayList<>();
    List<RewardPayout> targets = new ArrayList<>();

    for (UUID id : req.payoutIds()) {
      RewardPayout payout = rewardPayoutRepository.findById(id).orElse(null);
      if (payout == null) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getRewardReason() != GrantReason.TOP_LIKE) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getStatus() != RewardPayoutStatus.INFO_COLLECTED) {
        problematicIds.add(id);
        continue;
      }
      if (payout.getHoldStatus() != RewardPayoutHoldStatus.NONE) {
        problematicIds.add(id);
        continue;
      }
      if (req.expectedRevisions() != null && req.expectedRevisions().containsKey(id)) {
        if (!Objects.equals(payout.getRevision(), req.expectedRevisions().get(id))) {
          problematicIds.add(id);
          continue;
        }
      }
      targets.add(payout);
    }

    if (!problematicIds.isEmpty()) {
      throw new RewardPayoutConflictException("error.reward_payout.conflict", problematicIds);
    }

    for (RewardPayout payout : targets) {
      RewardPayoutStatus from = payout.getStatus();
      payout.markSent(req.sentAt(), req.reference(), req.note());
      executeTopLikePrizeGrant(payout);
      payout.incrementRevision();
      payout.addHistory(
          RewardPayoutAction.MARK_SENT, from, payout.getStatus(), actorId, req.note());
      rewardPayoutRepository.save(payout);
    }

    return req.payoutIds();
  }

  /** Retries execution for a failed payout without duplicate ledger grants. */
  @Transactional
  public RewardPayoutDetailResDto retry(UUID actorId, UUID payoutId) {
    RewardPayout payout =
        rewardPayoutRepository
            .findById(payoutId)
            .orElseThrow(() -> new ResourceNotFoundException("error.reward_payout.not_found"));

    if (payout.getHoldStatus() != RewardPayoutHoldStatus.NONE) {
      throw new ConflictException("error.reward_payout.hold_cannot_confirm");
    }
    if (payout.getStatus() == RewardPayoutStatus.PAID) {
      throw new ConflictException("error.reward_payout.already_paid");
    }

    payout.incrementRetryCount();
    RewardPayoutStatus from = payout.getStatus();

    if (payout.getRewardReason() == GrantReason.BASE) {
      executeBasePayoutGrant(payout);
      payout.markPaid(OffsetDateTime.now());
      payout.incrementRevision();
      payout.addHistory(
          RewardPayoutAction.RETRY, from, RewardPayoutStatus.PAID, actorId, "Retried base payout");
    } else {
      executeTopLikePrizeGrant(payout);
      payout.markSent(OffsetDateTime.now(), payout.getReference(), payout.getNote());
      payout.incrementRevision();
      payout.addHistory(
          RewardPayoutAction.RETRY, from, RewardPayoutStatus.SENT, actorId, "Retried prize payout");
    }

    rewardPayoutRepository.save(payout);
    return RewardPayoutDetailResDto.from(payout);
  }

  /** Puts payout on report hold. */
  @Transactional
  public RewardPayoutDetailResDto hold(UUID actorId, UUID payoutId, String note) {
    RewardPayout payout =
        rewardPayoutRepository
            .findById(payoutId)
            .orElseThrow(() -> new ResourceNotFoundException("error.reward_payout.not_found"));

    payout.hold(note);
    payout.incrementRevision();
    payout.addHistory(
        RewardPayoutAction.HOLD, payout.getStatus(), payout.getStatus(), actorId, note);
    rewardPayoutRepository.save(payout);
    return RewardPayoutDetailResDto.from(payout);
  }

  /** Releases hold status back to NONE. */
  @Transactional
  public RewardPayoutDetailResDto releaseHold(UUID actorId, UUID payoutId, String note) {
    RewardPayout payout =
        rewardPayoutRepository
            .findById(payoutId)
            .orElseThrow(() -> new ResourceNotFoundException("error.reward_payout.not_found"));

    payout.releaseHold(note);
    payout.incrementRevision();
    payout.addHistory(
        RewardPayoutAction.RELEASE_HOLD, payout.getStatus(), payout.getStatus(), actorId, note);
    rewardPayoutRepository.save(payout);
    return RewardPayoutDetailResDto.from(payout);
  }

  /** Creates or returns an existing BASE reward payout for a participation. */
  @Transactional
  public RewardPayout createBasePayout(
      UUID roundId,
      UUID eventId,
      UUID participationId,
      UUID userId,
      Integer points,
      String badgeCode) {
    return rewardPayoutRepository
        .findByParticipationIdAndRewardReason(participationId, GrantReason.BASE)
        .orElseGet(
            () -> {
              RewardPayout payout =
                  RewardPayout.builder()
                      .roundId(roundId)
                      .eventId(eventId)
                      .participationId(participationId)
                      .userId(userId)
                      .rewardReason(GrantReason.BASE)
                      .status(RewardPayoutStatus.PENDING_CONFIRM)
                      .holdStatus(RewardPayoutHoldStatus.NONE)
                      .points(points)
                      .badgeCode(badgeCode)
                      .build();
              payout.addHistory(
                  RewardPayoutAction.CREATE,
                  null,
                  RewardPayoutStatus.PENDING_CONFIRM,
                  null,
                  "Created base payout");
              return rewardPayoutRepository.save(payout);
            });
  }

  /** Creates or returns an existing TOP_LIKE reward payout for an excellence award winner. */
  @Transactional
  public RewardPayout createTopLikePayout(
      UUID roundId,
      UUID eventId,
      UUID participationId,
      UUID userId,
      Integer rankN,
      Integer likeCountAtClose,
      RewardCatalog catalog,
      String prizeName) {
    return rewardPayoutRepository
        .findByParticipationIdAndRewardReason(participationId, GrantReason.TOP_LIKE)
        .orElseGet(
            () -> {
              RewardPayoutStatus initialStatus =
                  (catalog != null || prizeName != null)
                      ? RewardPayoutStatus.PENDING_CONFIRM
                      : RewardPayoutStatus.PENDING_ASSIGN;
              RewardPayout payout =
                  RewardPayout.builder()
                      .roundId(roundId)
                      .eventId(eventId)
                      .participationId(participationId)
                      .userId(userId)
                      .rewardReason(GrantReason.TOP_LIKE)
                      .rankN(rankN)
                      .likeCountAtClose(likeCountAtClose)
                      .status(initialStatus)
                      .holdStatus(RewardPayoutHoldStatus.NONE)
                      .rewardCatalog(catalog)
                      .prizeName(prizeName)
                      .build();
              payout.addHistory(
                  RewardPayoutAction.CREATE, null, initialStatus, null, "Created top like payout");
              return rewardPayoutRepository.save(payout);
            });
  }

  private void executeBasePayoutGrant(RewardPayout payout) {
    rewardService.grantBaseReward(
        payout.getUserId(),
        payout.getParticipationId(),
        payout.getEventId(),
        payout.getPoints(),
        payout.getBadgeCode());
  }

  private void executeTopLikePrizeGrant(RewardPayout payout) {
    RewardCatalog prize = payout.getRewardCatalog();
    if (prize == null) {
      return;
    }
    if (rewardGrantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
        payout.getParticipationId(), GrantReason.TOP_LIKE, prize.getId())) {
      return;
    }
    RewardGrant grant =
        rewardGrantRepository.save(
            RewardGrant.builder()
                .userId(payout.getUserId())
                .reward(prize)
                .participationId(payout.getParticipationId())
                .eventId(payout.getEventId())
                .grantReason(GrantReason.TOP_LIKE)
                .grantedAt(OffsetDateTime.now())
                .build());
    if (prize.getStock() != null && prize.getStock() > 0) {
      prize.update(null, prize.getStock() - 1, null, null);
    }
    userCouponRepository.save(
        UserCoupon.builder()
            .userId(payout.getUserId())
            .reward(prize)
            .grantId(grant.getId())
            .expiresAt(
                prize.getValidDays() == null
                    ? null
                    : OffsetDateTime.now().plusDays(prize.getValidDays()))
            .build());
  }
}

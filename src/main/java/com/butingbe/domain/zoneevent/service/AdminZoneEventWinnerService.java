package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.auth.security.OperatorAuthorization;
import com.butingbe.domain.reward.entity.PayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.repository.RewardPayoutRepository;
import com.butingbe.domain.zoneevent.dto.request.ConfirmWinnersReqDto;
import com.butingbe.domain.zoneevent.dto.response.ConfirmWinnersResDto;
import com.butingbe.domain.zoneevent.dto.response.EventTopNResDto;
import com.butingbe.domain.zoneevent.dto.response.PayoutCandidateItemResDto;
import com.butingbe.domain.zoneevent.dto.response.PayoutGenerateResDto;
import com.butingbe.domain.zoneevent.dto.response.RoundTopNResDto;
import com.butingbe.domain.zoneevent.dto.response.TopNCandidateResDto;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuditLog;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventRankingSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEventRound;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuditLogRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRankingSnapshotRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Top N 랭킹 스냅샷 생성, 경계 동점자 관리자 확정, 보상 지급 후보 생성 관리 서비스. */
@Service
@RequiredArgsConstructor
public class AdminZoneEventWinnerService {

  private final ZoneEventRoundRepository roundRepository;
  private final ZoneEventRepository zoneEventRepository;
  private final ZoneEventParticipationRepository participationRepository;
  private final ZoneEventRankingSnapshotRepository snapshotRepository;
  private final ZoneEventReportRepository reportRepository;
  private final RewardPayoutRepository rewardPayoutRepository;
  private final ZoneEventAuditLogRepository auditLogRepository;
  private final OperatorAuthorization operatorAuthorization;

  @Transactional
  public RoundTopNResDto getTopN(AuthenticatedUser user, UUID roundId, UUID eventId) {
    operatorAuthorization.requireOperator(user);
    ZoneEventRound round =
        roundRepository
            .findById(roundId)
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));

    List<ZoneEvent> events;
    if (eventId != null) {
      ZoneEvent event =
          zoneEventRepository
              .findById(eventId)
              .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));
      if (!Objects.equals(roundId, event.getRoundId())) {
        throw new ConflictException("error.zone_event.invalid_state");
      }
      events = List.of(event);
    } else {
      events = zoneEventRepository.findByRoundId(roundId);
    }

    List<EventTopNResDto> eventDtos = new ArrayList<>();
    for (ZoneEvent event : events) {
      RewardSnapshot reward = event.getExcellenceReward();
      if (reward != null && reward.topN() != null && reward.topN() > 0) {
        eventDtos.add(resolveEventTopN(round, event));
      }
    }
    return new RoundTopNResDto(roundId, eventDtos);
  }

  @Transactional
  public ConfirmWinnersResDto confirmWinners(
      AuthenticatedUser user, UUID eventId, ConfirmWinnersReqDto request) {
    operatorAuthorization.requireOperator(user);
    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    if (request.expectedRevision() != null
        && !Objects.equals(request.expectedRevision(), event.getRevision())) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    ZoneEventRankingSnapshot refSnapshot =
        snapshotRepository
            .findById(request.snapshotId())
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));
    if (!Objects.equals(refSnapshot.getEventId(), eventId)) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    int version = refSnapshot.getVersion();
    List<ZoneEventRankingSnapshot> snapshots =
        snapshotRepository.findByEventIdAndVersionOrderByRankNAsc(eventId, version);

    int topN =
        (event.getExcellenceReward() != null && event.getExcellenceReward().topN() != null)
            ? event.getExcellenceReward().topN()
            : 0;

    List<ZoneEventRankingSnapshot> definitiveWinners =
        snapshots.stream().filter(s -> !s.getTied() && s.getRankN() <= topN).toList();
    List<ZoneEventRankingSnapshot> tiedCandidates =
        snapshots.stream().filter(ZoneEventRankingSnapshot::getTied).toList();

    int remainingSlots = Math.max(0, topN - definitiveWinners.size());
    List<UUID> requestedIds =
        request.participationIds() != null ? request.participationIds() : List.of();

    if (!tiedCandidates.isEmpty()) {
      Set<UUID> validTiedIds =
          tiedCandidates.stream()
              .map(ZoneEventRankingSnapshot::getParticipationId)
              .collect(Collectors.toSet());

      List<UUID> selectedTiedIds;
      if (requestedIds.size() == remainingSlots) {
        selectedTiedIds = requestedIds;
      } else {
        selectedTiedIds = requestedIds.stream().filter(validTiedIds::contains).toList();
        if (selectedTiedIds.size() != remainingSlots) {
          throw new ConflictException("error.zone_event.invalid_state");
        }
      }

      for (UUID id : selectedTiedIds) {
        if (!validTiedIds.contains(id)) {
          throw new ResourceNotFoundException("error.zone_event.participation.not_found");
        }
      }

      definitiveWinners.forEach(ZoneEventRankingSnapshot::markFinalized);
      tiedCandidates.stream()
          .filter(s -> selectedTiedIds.contains(s.getParticipationId()))
          .forEach(ZoneEventRankingSnapshot::markFinalized);
    } else {
      definitiveWinners.forEach(ZoneEventRankingSnapshot::markFinalized);
    }

    List<UUID> finalizedIds =
        snapshots.stream()
            .filter(ZoneEventRankingSnapshot::getFinalized)
            .map(ZoneEventRankingSnapshot::getParticipationId)
            .toList();

    auditLogRepository.save(
        ZoneEventAuditLog.builder()
            .actorId(user.id())
            .action("CONFIRM_WINNERS")
            .targetType("ZONE_EVENT")
            .targetId(eventId)
            .detail(
                Map.of(
                    "snapshotId", request.snapshotId().toString(),
                    "participationIds", finalizedIds.stream().map(UUID::toString).toList(),
                    "selectionReason", request.selectionReason(),
                    "version", version))
            .build());

    return new ConfirmWinnersResDto(
        eventId, version, finalizedIds, request.selectionReason(), event.getRevision());
  }

  @Transactional
  public PayoutGenerateResDto generatePayouts(AuthenticatedUser user, UUID eventId) {
    operatorAuthorization.requireOperator(user);
    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    List<ZoneEventRankingSnapshot> snapshots =
        snapshotRepository.findByEventIdAndVersionOrderByRankNAsc(eventId, 1);
    if (snapshots.isEmpty()) {
      throw new ResourceNotFoundException("error.zone_event.not_found");
    }

    List<ZoneEventRankingSnapshot> finalizedWinners =
        snapshots.stream().filter(ZoneEventRankingSnapshot::getFinalized).toList();
    if (finalizedWinners.isEmpty()) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    List<PayoutCandidateItemResDto> items = new ArrayList<>();
    int totalGenerated = 0;
    int heldCount = 0;

    for (ZoneEventRankingSnapshot winner : finalizedWinners) {
      var existingOpt = rewardPayoutRepository.findByParticipationId(winner.getParticipationId());
      RewardPayout payout;
      if (existingOpt.isPresent()) {
        payout = existingOpt.get();
      } else {
        boolean isHeld =
            reportRepository.countByParticipationIdAndStatusIn(
                    winner.getParticipationId(), List.of(ReportStatus.OPEN, ReportStatus.REVIEWING))
                > 0;
        RewardPayout newPayout =
            RewardPayout.builder()
                .eventId(eventId)
                .participationId(winner.getParticipationId())
                .rankN(winner.getRankN())
                .likeCountAtClose(winner.getLikeCountAtClose())
                .reward(event.getExcellenceReward())
                .build();
        if (isHeld) {
          newPayout.hold();
        }
        payout = rewardPayoutRepository.save(newPayout);
        totalGenerated++;
      }
      if (payout.getHoldStatus() == PayoutHoldStatus.HELD_REPORT) {
        heldCount++;
      }
      items.add(
          new PayoutCandidateItemResDto(
              payout.getId(),
              payout.getParticipationId(),
              payout.getRankN(),
              payout.getLikeCountAtClose(),
              payout.getStatus().name(),
              payout.getHoldStatus().name()));
    }

    return new PayoutGenerateResDto(eventId, totalGenerated, heldCount, items);
  }

  private EventTopNResDto resolveEventTopN(ZoneEventRound round, ZoneEvent event) {
    List<ZoneEventRankingSnapshot> snapshots =
        snapshotRepository.findByEventIdAndVersionOrderByRankNAsc(event.getId(), 1);
    if (snapshots.isEmpty()) {
      snapshots = freezeSnapshot(round, event);
    }

    boolean hasTiedBoundary = snapshots.stream().anyMatch(ZoneEventRankingSnapshot::getTied);
    boolean finalized = snapshots.stream().anyMatch(ZoneEventRankingSnapshot::getFinalized);
    UUID snapshotId = snapshots.isEmpty() ? null : snapshots.get(0).getId();
    Integer version = snapshots.isEmpty() ? 1 : snapshots.get(0).getVersion();
    OffsetDateTime closedAt =
        snapshots.isEmpty()
            ? (round.getClosedAt() != null ? round.getClosedAt() : round.getEndsAt())
            : snapshots.get(0).getClosedAt();

    List<TopNCandidateResDto> candidateDtos = new ArrayList<>();
    for (ZoneEventRankingSnapshot s : snapshots) {
      ZoneEventParticipation p =
          participationRepository.findById(s.getParticipationId()).orElse(null);
      long unresolvedReports =
          reportRepository.countByParticipationIdAndStatusIn(
              s.getParticipationId(), List.of(ReportStatus.OPEN, ReportStatus.REVIEWING));
      candidateDtos.add(
          new TopNCandidateResDto(
              s.getId(),
              s.getParticipationId(),
              p != null ? p.getUserId() : null,
              s.getRankN(),
              s.getLikeCountAtClose(),
              s.getTied(),
              s.getFinalized(),
              unresolvedReports > 0,
              unresolvedReports,
              p != null ? p.getMediaFileKey() : null,
              p != null
                  ? (p.getCompletedAt() != null ? p.getCompletedAt() : p.getJoinedAt())
                  : null));
    }

    return new EventTopNResDto(
        event.getId(),
        event.getZoneId(),
        event.getSlotCode(),
        event.getTitle(),
        event.getExcellenceReward().topN(),
        version,
        closedAt,
        hasTiedBoundary,
        finalized,
        snapshotId,
        candidateDtos);
  }

  private List<ZoneEventRankingSnapshot> freezeSnapshot(ZoneEventRound round, ZoneEvent event) {
    OffsetDateTime closedAt =
        round.getClosedAt() != null
            ? round.getClosedAt()
            : (round.getEndsAt() != null ? round.getEndsAt() : OffsetDateTime.now());

    int topN = event.getExcellenceReward().topN();
    List<ZoneEventParticipation> candidates =
        participationRepository.findTopPublicSuccessByEvent(event.getId(), PageRequest.of(0, 1000));

    if (candidates.isEmpty()) {
      return List.of();
    }

    int m = candidates.size();
    long boundaryLike = candidates.get(Math.min(topN, m) - 1).getLikeCount();
    long aboveCount = candidates.stream().filter(p -> p.getLikeCount() > boundaryLike).count();
    long equalCount = candidates.stream().filter(p -> p.getLikeCount() == boundaryLike).count();
    boolean hasTiedBoundary = (aboveCount + equalCount) > topN;

    List<ZoneEventRankingSnapshot> newSnapshots = new ArrayList<>();
    for (ZoneEventParticipation candidate : candidates) {
      int rankN =
          (int) candidates.stream().filter(c -> c.getLikeCount() > candidate.getLikeCount()).count()
              + 1;
      boolean tied = hasTiedBoundary && candidate.getLikeCount() == boundaryLike;

      if (rankN <= topN || tied) {
        ZoneEventRankingSnapshot snapshot =
            ZoneEventRankingSnapshot.builder()
                .eventId(event.getId())
                .closedAt(closedAt)
                .version(1)
                .participationId(candidate.getId())
                .rankN(rankN)
                .likeCountAtClose(candidate.getLikeCount())
                .tied(tied)
                .build();
        newSnapshots.add(snapshot);
      }
    }
    return snapshotRepository.saveAll(newSnapshots);
  }
}

package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.notification.service.NotificationService;
import com.butingbe.domain.reward.dto.response.SettlementItemStatus;
import com.butingbe.domain.reward.dto.response.TopLikeSettlementItemResDto;
import com.butingbe.domain.reward.dto.response.TopLikeSettlementReportResDto;
import com.butingbe.domain.reward.service.TopLikeSettlementService;
import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuditLog;
import com.butingbe.domain.zoneevent.entity.ZoneEventRound;
import com.butingbe.domain.zoneevent.entity.ZoneEventRoundSlot;
import com.butingbe.domain.zoneevent.entity.ZoneEventSettlementReport;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuditLogRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundSlotRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventSettlementReportRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Scheduler executing periodic round state transitions, activations, and settlements. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneEventRoundScheduler {

  private final ZoneEventRoundRepository roundRepository;
  private final ZoneEventRoundSlotRepository slotRepository;
  private final ZoneEventRepository zoneEventRepository;
  private final TopLikeSettlementService topLikeSettlementService;
  private final ZoneEventSettlementReportRepository settlementReportRepository;
  private final ZoneEventAuditLogRepository auditLogRepository;
  private final NotificationService notificationService;

  /** Checks and executes round openings and closures every 60 seconds. */
  @Scheduled(fixedDelay = 60000)
  @Transactional
  public void processRoundTransitions() {
    OffsetDateTime now = OffsetDateTime.now();
    openScheduledRounds(now);
    closeAndSettleRounds(now);
  }

  /** Opens rounds that reached their start time. */
  public void openScheduledRounds(OffsetDateTime now) {
    List<ZoneEventRound> scheduledRounds =
        roundRepository.findByStatusAndStartsAtLessThanEqual(RoundStatus.SCHEDULED, now).stream()
            .filter(round -> round.getEndsAt().isAfter(now))
            .toList();

    for (ZoneEventRound round : scheduledRounds) {
      round.open();
      List<ZoneEventRoundSlot> slots = slotRepository.findByRound_Id(round.getId());
      for (ZoneEventRoundSlot slot : slots) {
        if (slot.getEventId() != null) {
          zoneEventRepository
              .findById(slot.getEventId())
              .ifPresent(
                  event -> {
                    if (event.getStatus() == ZoneEventStatus.SCHEDULED) {
                      event.activate();
                    }
                  });
          notificationService.sendToZoneSubscribers(
              slot.getZoneId(), "새 구역 이벤트 시작", slot.getZoneId() + " 구역에서 새로운 인증 이벤트가 시작되었습니다.");
        }
      }

      auditLogRepository.save(
          ZoneEventAuditLog.builder()
              .action("ROUND_AUTO_OPEN")
              .targetType("ROUND")
              .targetId(round.getId().toString())
              .details("Automatically opened round at: " + now)
              .build());
    }
  }

  /** Closes and settles rounds that reached their end time. */
  public void closeAndSettleRounds(OffsetDateTime now) {
    List<ZoneEventRound> openRounds =
        roundRepository.findByStatusAndEndsAtLessThanEqual(RoundStatus.OPEN, now);

    for (ZoneEventRound round : openRounds) {
      round.close();
      List<ZoneEvent> events = zoneEventRepository.findByRoundId(round.getId());
      for (ZoneEvent event : events) {
        if (event.getStatus() == ZoneEventStatus.ACTIVE) {
          event.close();
        }
      }

      List<TopLikeSettlementReportResDto> settlementReports =
          topLikeSettlementService.settleRound(round.getId());

      round.settle(now);

      int totalCandidates = 0;
      int totalGranted = 0;
      int totalSkippedStock = 0;
      int totalSkippedCap = 0;

      for (TopLikeSettlementReportResDto report : settlementReports) {
        totalCandidates += report.totalCandidates();
        totalGranted += report.totalGranted();
        totalSkippedStock += report.totalSkippedStock();
        totalSkippedCap += report.totalSkippedMonthlyCap();

        for (TopLikeSettlementItemResDto item : report.items()) {
          if (item.status() == SettlementItemStatus.GRANTED) {
            notificationService.sendToUser(
                item.userId(),
                "우수 보상 선정 안내",
                "축하합니다! 구역 이벤트 우수 참여자로 선정되어 보상이 지급되었습니다.",
                "SETTLEMENT");
          }
        }
      }

      java.util.Map<String, Object> summary = new java.util.HashMap<>();
      summary.put("totalEvents", events.size());
      summary.put("totalCandidates", totalCandidates);
      summary.put("totalGranted", totalGranted);
      summary.put("totalSkippedStock", totalSkippedStock);
      summary.put("totalSkippedCap", totalSkippedCap);
      summary.put("settledAt", now.toString());

      settlementReportRepository.save(
          ZoneEventSettlementReport.builder().roundId(round.getId()).summaryJson(summary).build());

      auditLogRepository.save(
          ZoneEventAuditLog.builder()
              .action("ROUND_AUTO_SETTLE")
              .targetType("ROUND")
              .targetId(round.getId().toString())
              .details("Auto-settled round. Total granted: " + totalGranted)
              .build());
    }
  }
}

package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.butingbe.domain.notification.service.NotificationService;
import com.butingbe.domain.reward.dto.response.SettlementItemStatus;
import com.butingbe.domain.reward.dto.response.TopLikeSettlementItemResDto;
import com.butingbe.domain.reward.dto.response.TopLikeSettlementReportResDto;
import com.butingbe.domain.reward.service.TopLikeSettlementService;
import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.RoundType;
import com.butingbe.domain.zoneevent.entity.SlotKind;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ZoneEventRoundSchedulerTest {

  private static final UUID ROUND_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID EVENT_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");
  private static final UUID USER_ID = UUID.fromString("33333333-0000-0000-0000-000000000003");

  @Mock private ZoneEventRoundRepository roundRepository;
  @Mock private ZoneEventRoundSlotRepository slotRepository;
  @Mock private ZoneEventRepository zoneEventRepository;
  @Mock private TopLikeSettlementService topLikeSettlementService;
  @Mock private ZoneEventSettlementReportRepository settlementReportRepository;
  @Mock private ZoneEventAuditLogRepository auditLogRepository;
  @Mock private NotificationService notificationService;

  @InjectMocks private ZoneEventRoundScheduler scheduler;

  @Test
  @DisplayName("openScheduledRounds opens scheduled rounds and activates events")
  void openScheduledRounds_success() {
    OffsetDateTime now = OffsetDateTime.now();
    ZoneEventRound round =
        ZoneEventRound.builder()
            .roundType(RoundType.REGULAR)
            .startsAt(now.minusMinutes(5))
            .endsAt(now.plusDays(7))
            .status(RoundStatus.SCHEDULED)
            .build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);

    ZoneEvent event = ZoneEvent.builder().title("이벤트").status(ZoneEventStatus.SCHEDULED).build();
    ReflectionTestUtils.setField(event, "id", EVENT_ID);

    ZoneEventRoundSlot slot =
        ZoneEventRoundSlot.builder()
            .round(round)
            .zoneId("GWANGAN")
            .slotKind(SlotKind.AUTH)
            .eventId(EVENT_ID)
            .build();

    when(roundRepository.findByStatusAndStartsAtLessThanEqual(eq(RoundStatus.SCHEDULED), any()))
        .thenReturn(List.of(round));
    when(slotRepository.findByRound_Id(ROUND_ID)).thenReturn(List.of(slot));
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

    scheduler.openScheduledRounds(now);

    assertThat(round.getStatus()).isEqualTo(RoundStatus.OPEN);
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.ACTIVE);
    verify(notificationService).sendToZoneSubscribers(eq("GWANGAN"), any(), any());
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
  }

  @Test
  @DisplayName("closeAndSettleRounds settles expired rounds and grants rewards")
  void closeAndSettleRounds_success() {
    OffsetDateTime now = OffsetDateTime.now();
    ZoneEventRound round =
        ZoneEventRound.builder()
            .roundType(RoundType.REGULAR)
            .startsAt(now.minusDays(7))
            .endsAt(now.minusMinutes(1))
            .status(RoundStatus.OPEN)
            .build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);

    ZoneEvent event = ZoneEvent.builder().title("이벤트").status(ZoneEventStatus.ACTIVE).build();
    ReflectionTestUtils.setField(event, "id", EVENT_ID);

    TopLikeSettlementItemResDto item =
        new TopLikeSettlementItemResDto(
            EVENT_ID,
            UUID.randomUUID(),
            USER_ID,
            1,
            10L,
            "PRIZE_COUPON",
            SettlementItemStatus.GRANTED,
            UUID.randomUUID(),
            null);
    TopLikeSettlementReportResDto settlementReport =
        new TopLikeSettlementReportResDto(EVENT_ID, 1, 1, 0, 0, List.of(item));

    when(roundRepository.findByStatusAndEndsAtLessThanEqual(eq(RoundStatus.OPEN), any()))
        .thenReturn(List.of(round));
    when(zoneEventRepository.findByRoundId(ROUND_ID)).thenReturn(List.of(event));
    when(topLikeSettlementService.settleRound(ROUND_ID)).thenReturn(List.of(settlementReport));

    scheduler.closeAndSettleRounds(now);

    assertThat(round.getStatus()).isEqualTo(RoundStatus.SETTLED);
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.CLOSED);
    verify(notificationService).sendToUser(eq(USER_ID), any(), any(), eq("SETTLEMENT"));
    verify(settlementReportRepository).save(any(ZoneEventSettlementReport.class));
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
  }

  @Test
  @DisplayName("processRoundTransitions executes both open and close routines")
  void processRoundTransitions_test() {
    when(roundRepository.findByStatusAndStartsAtLessThanEqual(any(), any())).thenReturn(List.of());
    when(roundRepository.findByStatusAndEndsAtLessThanEqual(any(), any())).thenReturn(List.of());

    scheduler.processRoundTransitions();

    verify(roundRepository).findByStatusAndStartsAtLessThanEqual(any(), any());
    verify(roundRepository).findByStatusAndEndsAtLessThanEqual(any(), any());
  }
}

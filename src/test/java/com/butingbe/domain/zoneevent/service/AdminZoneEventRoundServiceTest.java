package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.butingbe.domain.zoneevent.dto.request.BackupTargetCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.RainTargetReplaceReqDto;
import com.butingbe.domain.zoneevent.dto.request.RoundCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.SlotCreateReqDto;
import com.butingbe.domain.zoneevent.dto.response.AutoAssignRecommendationResDto;
import com.butingbe.domain.zoneevent.dto.response.PublicCurrentRoundResDto;
import com.butingbe.domain.zoneevent.dto.response.RoundResDto;
import com.butingbe.domain.zoneevent.dto.response.SlotResDto;
import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.RoundType;
import com.butingbe.domain.zoneevent.entity.SlotKind;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuditLog;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventBackupTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventRound;
import com.butingbe.domain.zoneevent.entity.ZoneEventRoundSlot;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuditLogRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuthTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventBackupTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundSlotRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminZoneEventRoundServiceTest {

  private static final UUID OPERATOR_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID ROUND_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");
  private static final UUID SLOT_ID = UUID.fromString("33333333-0000-0000-0000-000000000003");
  private static final UUID EVENT_ID = UUID.fromString("44444444-0000-0000-0000-000000000004");
  private static final UUID BACKUP_ID = UUID.fromString("55555555-0000-0000-0000-000000000005");

  @Mock private ZoneEventRoundRepository roundRepository;
  @Mock private ZoneEventRoundSlotRepository slotRepository;
  @Mock private ZoneEventBackupTargetRepository backupTargetRepository;
  @Mock private ZoneEventRepository zoneEventRepository;
  @Mock private ZoneEventAuthTargetRepository authTargetRepository;
  @Mock private ZoneEventAuditLogRepository auditLogRepository;

  @InjectMocks private AdminZoneEventRoundService roundService;

  @Test
  @DisplayName("createRound throws IllegalArgumentException on invalid time period")
  void createRound_invalidPeriod() {
    OffsetDateTime now = OffsetDateTime.now();
    RoundCreateReqDto req =
        new RoundCreateReqDto(
            RoundType.REGULAR, now, now.minusDays(1), "Asia/Seoul", List.of(), List.of());

    assertThatThrownBy(() -> roundService.createRound(req, OPERATOR_ID))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("createRound creates round with slots and backup targets")
  void createRound_success() {
    OffsetDateTime start = OffsetDateTime.now();
    OffsetDateTime end = start.plusDays(7);

    SlotCreateReqDto slotReq = new SlotCreateReqDto("GWANGAN", SlotKind.AUTH, EVENT_ID, null);
    BackupTargetCreateReqDto btReq =
        new BackupTargetCreateReqDto(
            com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind.PLACE,
            null,
            "우천대체",
            "가이드",
            "key.jpg",
            35.15,
            129.11,
            50);
    RoundCreateReqDto req =
        new RoundCreateReqDto(
            RoundType.REGULAR, start, end, "Asia/Seoul", List.of(slotReq), List.of(btReq));

    ZoneEventRound round =
        ZoneEventRound.builder()
            .roundType(RoundType.REGULAR)
            .startsAt(start)
            .endsAt(end)
            .timezone("Asia/Seoul")
            .status(RoundStatus.SCHEDULED)
            .build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);

    ZoneEventRoundSlot slot =
        ZoneEventRoundSlot.builder()
            .round(round)
            .zoneId("GWANGAN")
            .slotKind(SlotKind.AUTH)
            .eventId(EVENT_ID)
            .build();
    ReflectionTestUtils.setField(slot, "id", SLOT_ID);

    ZoneEventBackupTarget bt =
        ZoneEventBackupTarget.builder()
            .round(round)
            .targetKind(com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind.PLACE)
            .placeName("우천대체")
            .build();
    ReflectionTestUtils.setField(bt, "id", BACKUP_ID);

    when(roundRepository.save(any(ZoneEventRound.class))).thenReturn(round);
    when(slotRepository.save(any(ZoneEventRoundSlot.class))).thenReturn(slot);
    when(backupTargetRepository.save(any(ZoneEventBackupTarget.class))).thenReturn(bt);

    RoundResDto result = roundService.createRound(req, OPERATOR_ID);

    assertThat(result.roundId()).isEqualTo(ROUND_ID);
    assertThat(result.slots()).hasSize(1);
    assertThat(result.backupTargets()).hasSize(1);
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
  }

  @Test
  @DisplayName("getRound throws ResourceNotFoundException if not found")
  void getRound_notFound() {
    when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roundService.getRound(ROUND_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getRound returns round with slots and backup targets")
  void getRound_success() {
    ZoneEventRound round =
        ZoneEventRound.builder()
            .roundType(RoundType.REGULAR)
            .startsAt(OffsetDateTime.now())
            .endsAt(OffsetDateTime.now().plusDays(7))
            .timezone("Asia/Seoul")
            .status(RoundStatus.OPEN)
            .build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);

    when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));
    when(slotRepository.findByRound_Id(ROUND_ID)).thenReturn(List.of());
    when(backupTargetRepository.findByRound_Id(ROUND_ID)).thenReturn(List.of());

    RoundResDto result = roundService.getRound(ROUND_ID);
    assertThat(result.roundId()).isEqualTo(ROUND_ID);
    assertThat(result.status()).isEqualTo(RoundStatus.OPEN);
  }

  @Test
  @DisplayName("listRounds returns paginated round DTOs")
  void listRounds_success() {
    ZoneEventRound round =
        ZoneEventRound.builder()
            .roundType(RoundType.REGULAR)
            .startsAt(OffsetDateTime.now())
            .endsAt(OffsetDateTime.now().plusDays(7))
            .timezone("Asia/Seoul")
            .status(RoundStatus.OPEN)
            .build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);

    when(roundRepository.findAll(any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(round), PageRequest.of(0, 10), 1));
    when(roundRepository.findById(ROUND_ID)).thenReturn(Optional.of(round));

    Page<RoundResDto> result = roundService.listRounds(PageRequest.of(0, 10));
    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  @DisplayName("replaceSlot fails if round already settled")
  void replaceSlot_alreadySettled() {
    ZoneEventRound round = ZoneEventRound.builder().status(RoundStatus.SETTLED).build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);
    ZoneEventRoundSlot slot = ZoneEventRoundSlot.builder().round(round).build();
    ReflectionTestUtils.setField(slot, "id", SLOT_ID);

    when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));

    assertThatThrownBy(() -> roundService.replaceSlot(ROUND_ID, SLOT_ID, EVENT_ID, OPERATOR_ID))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("replaceSlot fails on zone mismatch")
  void replaceSlot_zoneMismatch() {
    ZoneEventRound round = ZoneEventRound.builder().status(RoundStatus.OPEN).build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);
    ZoneEventRoundSlot slot = ZoneEventRoundSlot.builder().round(round).zoneId("GWANGAN").build();
    ReflectionTestUtils.setField(slot, "id", SLOT_ID);

    ZoneEvent newEvent = ZoneEvent.builder().zoneId("HAEUNDAE").build();
    ReflectionTestUtils.setField(newEvent, "id", EVENT_ID);

    when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(newEvent));

    assertThatThrownBy(() -> roundService.replaceSlot(ROUND_ID, SLOT_ID, EVENT_ID, OPERATOR_ID))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("replaceSlot succeeds and logs audit")
  void replaceSlot_success() {
    ZoneEventRound round = ZoneEventRound.builder().status(RoundStatus.OPEN).build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);
    ZoneEventRoundSlot slot =
        ZoneEventRoundSlot.builder().round(round).zoneId("GWANGAN").slotKind(SlotKind.AUTH).build();
    ReflectionTestUtils.setField(slot, "id", SLOT_ID);

    ZoneEvent newEvent =
        ZoneEvent.builder().zoneId("GWANGAN").title("새 이벤트").status(ZoneEventStatus.ACTIVE).build();
    ReflectionTestUtils.setField(newEvent, "id", EVENT_ID);

    when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(newEvent));

    SlotResDto result = roundService.replaceSlot(ROUND_ID, SLOT_ID, EVENT_ID, OPERATOR_ID);

    assertThat(result.eventId()).isEqualTo(EVENT_ID);
    assertThat(result.eventTitle()).isEqualTo("새 이벤트");
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
  }

  @Test
  @DisplayName("replaceRainTarget swaps active auth target with backup")
  void replaceRainTarget_success() {
    ZoneEventRound round = ZoneEventRound.builder().status(RoundStatus.OPEN).build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);

    ZoneEventBackupTarget bt =
        ZoneEventBackupTarget.builder()
            .round(round)
            .placeName("우천 실내")
            .guideText("안내문")
            .exampleFileKey("ex.jpg")
            .latitude(35.15)
            .longitude(129.11)
            .radiusM(60)
            .build();
    ReflectionTestUtils.setField(bt, "id", BACKUP_ID);

    ZoneEventAuthTarget target =
        ZoneEventAuthTarget.builder()
            .placeName("원래 실외")
            .guideText("원래안내")
            .latitude(35.14)
            .longitude(129.10)
            .radiusM(50)
            .build();
    ReflectionTestUtils.setField(target, "id", UUID.randomUUID());

    when(backupTargetRepository.findById(BACKUP_ID)).thenReturn(Optional.of(bt));
    when(authTargetRepository.findByEvent_Id(EVENT_ID)).thenReturn(Optional.of(target));

    RainTargetReplaceReqDto req = new RainTargetReplaceReqDto(BACKUP_ID, EVENT_ID);
    roundService.replaceRainTarget(ROUND_ID, req, OPERATOR_ID);

    assertThat(target.getPlaceName()).isEqualTo("우천 실내");
    assertThat(target.getRadiusM()).isEqualTo(60);
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
  }

  @Test
  @DisplayName("recommendAutoAssign generates recommendations for all zones")
  void recommendAutoAssign_success() {
    when(slotRepository.findAll()).thenReturn(List.of());

    AutoAssignRecommendationResDto result = roundService.recommendAutoAssign();

    assertThat(result.recommendations()).isNotEmpty();
    assertThat(result.recommendations().get(0).priorityScore()).isGreaterThan(0);
  }

  @Test
  @DisplayName("getCurrentActiveRound returns currently open round")
  void getCurrentActiveRound_success() {
    ZoneEventRound round =
        ZoneEventRound.builder()
            .roundType(RoundType.REGULAR)
            .startsAt(OffsetDateTime.now().minusDays(1))
            .endsAt(OffsetDateTime.now().plusDays(6))
            .status(RoundStatus.OPEN)
            .build();
    ReflectionTestUtils.setField(round, "id", ROUND_ID);

    when(roundRepository.findFirstByStatusOrderByStartsAtDesc(RoundStatus.OPEN))
        .thenReturn(Optional.of(round));
    when(slotRepository.findByRound_Id(ROUND_ID)).thenReturn(List.of());

    PublicCurrentRoundResDto result = roundService.getCurrentActiveRound();

    assertThat(result.roundId()).isEqualTo(ROUND_ID);
    assertThat(result.status()).isEqualTo(RoundStatus.OPEN);
  }
}

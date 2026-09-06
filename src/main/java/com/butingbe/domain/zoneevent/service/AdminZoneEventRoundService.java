package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.chat.entity.ChatZone;
import com.butingbe.domain.zoneevent.dto.request.BackupTargetCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.RainTargetReplaceReqDto;
import com.butingbe.domain.zoneevent.dto.request.RoundCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.SlotCreateReqDto;
import com.butingbe.domain.zoneevent.dto.response.AutoAssignRecommendationResDto;
import com.butingbe.domain.zoneevent.dto.response.BackupTargetResDto;
import com.butingbe.domain.zoneevent.dto.response.PublicCurrentRoundResDto;
import com.butingbe.domain.zoneevent.dto.response.RoundResDto;
import com.butingbe.domain.zoneevent.dto.response.SlotResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneRecommendationResDto;
import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.RoundType;
import com.butingbe.domain.zoneevent.entity.SlotKind;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuditLog;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventBackupTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventRound;
import com.butingbe.domain.zoneevent.entity.ZoneEventRoundSlot;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuditLogRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuthTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventBackupTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundSlotRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrative service for round creation, slot assignments, and rain replacements. */
@Service
@RequiredArgsConstructor
public class AdminZoneEventRoundService {

  private final ZoneEventRoundRepository roundRepository;
  private final ZoneEventRoundSlotRepository slotRepository;
  private final ZoneEventBackupTargetRepository backupTargetRepository;
  private final ZoneEventRepository zoneEventRepository;
  private final ZoneEventAuthTargetRepository authTargetRepository;
  private final ZoneEventAuditLogRepository auditLogRepository;

  /** Creates a new operating round with configured slots and backup targets. */
  @Transactional
  public RoundResDto createRound(RoundCreateReqDto request, UUID operatorId) {
    if (!request.startsAt().isBefore(request.endsAt())) {
      throw new IllegalArgumentException("error.zone_event.round.invalid_period");
    }

    ZoneEventRound round =
        roundRepository.save(
            ZoneEventRound.builder()
                .roundType(request.roundType() == null ? RoundType.REGULAR : request.roundType())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .timezone(request.timezone() == null ? "Asia/Seoul" : request.timezone())
                .status(RoundStatus.SCHEDULED)
                .build());

    List<SlotResDto> slotDtos = new ArrayList<>();
    if (request.slots() != null) {
      for (SlotCreateReqDto slotReq : request.slots()) {
        ZoneEventRoundSlot slot =
            slotRepository.save(
                ZoneEventRoundSlot.builder()
                    .round(round)
                    .slotKind(slotReq.slotKind())
                    .zoneId(slotReq.zoneId())
                    .eventId(slotReq.eventId())
                    .pairId(slotReq.pairId())
                    .build());
        slotDtos.add(toSlotDto(slot));
      }
    }

    List<BackupTargetResDto> backupDtos = new ArrayList<>();
    if (request.backupTargets() != null) {
      for (BackupTargetCreateReqDto btReq : request.backupTargets()) {
        ZoneEventBackupTarget bt =
            backupTargetRepository.save(
                ZoneEventBackupTarget.builder()
                    .round(round)
                    .targetKind(btReq.targetKind())
                    .landmarkId(btReq.landmarkId())
                    .placeName(btReq.placeName())
                    .guideText(btReq.guideText())
                    .exampleFileKey(btReq.exampleFileKey())
                    .latitude(btReq.latitude())
                    .longitude(btReq.longitude())
                    .radiusM(btReq.radiusM())
                    .build());
        backupDtos.add(BackupTargetResDto.from(bt));
      }
    }

    auditLogRepository.save(
        ZoneEventAuditLog.builder()
            .operatorId(operatorId)
            .action("ROUND_CREATE")
            .targetType("ROUND")
            .targetId(round.getId().toString())
            .details("Created round: " + round.getStartsAt() + " - " + round.getEndsAt())
            .build());

    return RoundResDto.of(round, slotDtos, backupDtos);
  }

  /** Retrieves full details of a specific round. */
  @Transactional(readOnly = true)
  public RoundResDto getRound(UUID roundId) {
    ZoneEventRound round =
        roundRepository
            .findById(roundId)
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));

    List<SlotResDto> slots =
        slotRepository.findByRound_Id(roundId).stream()
            .map(this::toSlotDto)
            .collect(Collectors.toList());

    List<BackupTargetResDto> backupTargets =
        backupTargetRepository.findByRound_Id(roundId).stream()
            .map(BackupTargetResDto::from)
            .collect(Collectors.toList());

    return RoundResDto.of(round, slots, backupTargets);
  }

  /** Lists rounds with pagination. */
  @Transactional(readOnly = true)
  public Page<RoundResDto> listRounds(Pageable pageable) {
    return roundRepository.findAll(pageable).map(round -> getRound(round.getId()));
  }

  /** Replaces an assigned event within a round slot. */
  @Transactional
  public SlotResDto replaceSlot(UUID roundId, UUID slotId, UUID newEventId, UUID operatorId) {
    ZoneEventRoundSlot slot =
        slotRepository
            .findById(slotId)
            .filter(s -> s.getRound().getId().equals(roundId))
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));

    if (slot.getRound().getStatus() == RoundStatus.SETTLED) {
      throw new ConflictException("error.zone_event.round.already_settled");
    }

    ZoneEvent newEvent =
        zoneEventRepository
            .findById(newEventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    if (!slot.getZoneId().equals(newEvent.getZoneId())) {
      throw new IllegalArgumentException("error.zone_event.zone_mismatch");
    }

    slot.assignEvent(newEventId);

    auditLogRepository.save(
        ZoneEventAuditLog.builder()
            .operatorId(operatorId)
            .action("SLOT_REPLACE")
            .targetType("SLOT")
            .targetId(slotId.toString())
            .details("Replaced event in slot with eventId: " + newEventId)
            .build());

    return toSlotDto(slot);
  }

  /** Immediately swaps an event's active auth target with a registered backup target. */
  @Transactional
  public void replaceRainTarget(UUID roundId, RainTargetReplaceReqDto request, UUID operatorId) {
    ZoneEventBackupTarget backupTarget =
        backupTargetRepository
            .findById(request.backupTargetId())
            .filter(bt -> bt.getRound().getId().equals(roundId))
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));

    ZoneEventAuthTarget target =
        authTargetRepository
            .findByEvent_Id(request.targetEventId())
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    target.update(
        backupTarget.getPlaceName(),
        backupTarget.getGuideText(),
        backupTarget.getExampleFileKey(),
        backupTarget.getLatitude(),
        backupTarget.getLongitude(),
        backupTarget.getRadiusM());

    auditLogRepository.save(
        ZoneEventAuditLog.builder()
            .operatorId(operatorId)
            .action("RAIN_TARGET_REPLACE")
            .targetType("AUTH_TARGET")
            .targetId(target.getId().toString())
            .details("Swapped target for event: " + request.targetEventId())
            .build());
  }

  /** Generates automated zone recommendations based on past round rotation distribution. */
  @Transactional(readOnly = true)
  public AutoAssignRecommendationResDto recommendAutoAssign() {
    List<ZoneEventRoundSlot> recentSlots = slotRepository.findAll();
    List<ZoneRecommendationResDto> list = new ArrayList<>();

    List<String> allZones =
        Arrays.stream(ChatZone.values()).map(Enum::name).collect(Collectors.toList());

    for (String zoneId : allZones) {
      long appearances = recentSlots.stream().filter(s -> zoneId.equals(s.getZoneId())).count();
      int priorityScore = (int) Math.max(10, 100 - (appearances * 15));
      SlotKind suggested = SlotKind.AUTH;
      String reason = "Zone rotation score: " + priorityScore + " based on historical appearances";
      list.add(new ZoneRecommendationResDto(zoneId, suggested, priorityScore, reason));
    }

    list.sort(Comparator.comparingInt(ZoneRecommendationResDto::priorityScore).reversed());
    return new AutoAssignRecommendationResDto(list);
  }

  /** Retrieves the currently open or active round for public mobile display. */
  @Transactional(readOnly = true)
  public PublicCurrentRoundResDto getCurrentActiveRound() {
    OffsetDateTime now = OffsetDateTime.now();
    ZoneEventRound currentRound =
        roundRepository
            .findFirstByStatusOrderByStartsAtDesc(RoundStatus.OPEN)
            .or(() -> roundRepository.findFirstByStatusOrderByStartsAtDesc(RoundStatus.SCHEDULED))
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));

    List<SlotResDto> slots =
        slotRepository.findByRound_Id(currentRound.getId()).stream()
            .map(this::toSlotDto)
            .collect(Collectors.toList());

    return new PublicCurrentRoundResDto(
        currentRound.getId(),
        currentRound.getRoundType(),
        currentRound.getStartsAt(),
        currentRound.getEndsAt(),
        currentRound.getStatus(),
        slots);
  }

  private SlotResDto toSlotDto(ZoneEventRoundSlot slot) {
    String title = null;
    com.butingbe.domain.zoneevent.entity.ZoneEventStatus status = null;
    if (slot.getEventId() != null) {
      ZoneEvent event = zoneEventRepository.findById(slot.getEventId()).orElse(null);
      if (event != null) {
        title = event.getTitle();
        status = event.getStatus();
      }
    }
    return new SlotResDto(
        slot.getId(),
        slot.getZoneId(),
        slot.getSlotKind(),
        slot.getEventId(),
        slot.getPairId(),
        title,
        status);
  }
}

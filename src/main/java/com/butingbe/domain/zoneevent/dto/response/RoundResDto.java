package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.RoundType;
import com.butingbe.domain.zoneevent.entity.ZoneEventRound;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Response DTO representing an operating round and its configured slots and backup targets. */
public record RoundResDto(
    UUID roundId,
    RoundType roundType,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    String timezone,
    RoundStatus status,
    OffsetDateTime settledAt,
    List<SlotResDto> slots,
    List<BackupTargetResDto> backupTargets) {

  public static RoundResDto of(
      ZoneEventRound round, List<SlotResDto> slots, List<BackupTargetResDto> backupTargets) {
    return new RoundResDto(
        round.getId(),
        round.getRoundType(),
        round.getStartsAt(),
        round.getEndsAt(),
        round.getTimezone(),
        round.getStatus(),
        round.getSettledAt(),
        slots == null ? List.of() : slots,
        backupTargets == null ? List.of() : backupTargets);
  }
}

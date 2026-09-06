package com.butingbe.domain.zoneevent.dto.request;

import com.butingbe.domain.zoneevent.entity.RoundType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

/** Request DTO for creating a new operating zone event round. */
public record RoundCreateReqDto(
    RoundType roundType,
    @NotNull OffsetDateTime startsAt,
    @NotNull OffsetDateTime endsAt,
    String timezone,
    @Valid List<SlotCreateReqDto> slots,
    @Valid List<BackupTargetCreateReqDto> backupTargets) {}

package com.butingbe.domain.zoneevent.dto.request;

import com.butingbe.domain.zoneevent.entity.SlotKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** DTO for declaring a zone slot when creating a round. */
public record SlotCreateReqDto(
    @NotBlank String zoneId, @NotNull SlotKind slotKind, UUID eventId, UUID pairId) {}

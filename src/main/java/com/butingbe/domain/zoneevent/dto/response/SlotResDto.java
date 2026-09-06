package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.SlotKind;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import java.util.UUID;

/** Response DTO representing an assigned slot within a round. */
public record SlotResDto(
    UUID slotId,
    String zoneId,
    SlotKind slotKind,
    UUID eventId,
    UUID pairId,
    String eventTitle,
    ZoneEventStatus eventStatus) {}

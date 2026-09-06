package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.SlotKind;

/** Recommended zone selection for round slot assignment. */
public record ZoneRecommendationResDto(
    String zoneId, SlotKind suggestedSlotKind, int priorityScore, String reason) {}

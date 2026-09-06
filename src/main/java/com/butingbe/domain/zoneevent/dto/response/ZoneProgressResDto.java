package com.butingbe.domain.zoneevent.dto.response;

/** Progress report towards next title tier in a zone. */
public record ZoneProgressResDto(
    String zoneId,
    int currentTier,
    long accumulatedSuccessCount,
    Integer nextTierRequiredCount,
    Integer remainingToNextTier) {}

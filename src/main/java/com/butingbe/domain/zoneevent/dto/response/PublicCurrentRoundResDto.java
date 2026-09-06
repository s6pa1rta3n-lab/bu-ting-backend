package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.RoundType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Public lightweight round summary for mobile client display. */
public record PublicCurrentRoundResDto(
    UUID roundId,
    RoundType roundType,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    RoundStatus status,
    List<SlotResDto> slots) {}

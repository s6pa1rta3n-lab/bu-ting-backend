package com.butingbe.domain.zoneevent.dto.request;

import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import jakarta.validation.constraints.NotNull;

/** Request DTO for updating participation visibility (PUBLIC / PRIVATE). */
public record VisibilityUpdateReqDto(@NotNull ParticipationVisibility visibility) {}

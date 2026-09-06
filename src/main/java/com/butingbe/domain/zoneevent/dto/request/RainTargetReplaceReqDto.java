package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request DTO for immediately replacing an event's target with a registered backup target. */
public record RainTargetReplaceReqDto(@NotNull UUID backupTargetId, @NotNull UUID targetEventId) {}

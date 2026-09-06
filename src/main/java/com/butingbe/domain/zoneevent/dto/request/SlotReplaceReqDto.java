package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request DTO for replacing an event assigned to a slot. */
public record SlotReplaceReqDto(@NotNull UUID newEventId) {}

package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request DTO for equipping a user zone title. */
public record EquipTitleReqDto(@NotNull UUID userTitleId) {}

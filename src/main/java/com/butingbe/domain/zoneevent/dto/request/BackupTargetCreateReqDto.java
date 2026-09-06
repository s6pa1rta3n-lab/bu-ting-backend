package com.butingbe.domain.zoneevent.dto.request;

import com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** DTO for registering a round rain backup target. */
public record BackupTargetCreateReqDto(
    @NotNull ZoneEventTargetKind targetKind,
    String landmarkId,
    @NotBlank String placeName,
    String guideText,
    String exampleFileKey,
    @NotNull Double latitude,
    @NotNull Double longitude,
    @NotNull Integer radiusM) {}

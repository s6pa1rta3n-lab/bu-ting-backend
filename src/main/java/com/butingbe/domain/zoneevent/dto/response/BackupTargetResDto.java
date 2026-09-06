package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.ZoneEventBackupTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind;
import java.util.UUID;

/** Response DTO representing a round backup target. */
public record BackupTargetResDto(
    UUID targetId,
    UUID roundId,
    ZoneEventTargetKind targetKind,
    String landmarkId,
    String placeName,
    String guideText,
    String exampleFileKey,
    Double latitude,
    Double longitude,
    Integer radiusM) {

  public static BackupTargetResDto from(ZoneEventBackupTarget target) {
    return new BackupTargetResDto(
        target.getId(),
        target.getRound().getId(),
        target.getTargetKind(),
        target.getLandmarkId(),
        target.getPlaceName(),
        target.getGuideText(),
        target.getExampleFileKey(),
        target.getLatitude(),
        target.getLongitude(),
        target.getRadiusM());
  }
}

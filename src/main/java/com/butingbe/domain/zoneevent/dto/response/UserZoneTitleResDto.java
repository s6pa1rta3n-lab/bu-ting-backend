package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.UserZoneTitle;
import java.time.OffsetDateTime;

/** Response DTO representing an earned user zone title. */
public record UserZoneTitleResDto(
    String userTitleId, ZoneTitleDefResDto titleDef, boolean isEquipped, OffsetDateTime earnedAt) {

  public static UserZoneTitleResDto from(UserZoneTitle userTitle) {
    return new UserZoneTitleResDto(
        userTitle.getId().toString(),
        ZoneTitleDefResDto.from(userTitle.getTitleDef()),
        userTitle.getIsEquipped(),
        userTitle.getEarnedAt());
  }
}

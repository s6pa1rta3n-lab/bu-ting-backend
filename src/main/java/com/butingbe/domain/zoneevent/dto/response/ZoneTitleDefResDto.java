package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.ZoneTitleDef;

/** Response DTO representing a zone title definition. */
public record ZoneTitleDefResDto(
    String titleId,
    String zoneId,
    int tier,
    int requiredSuccessCount,
    String titleCode,
    String titleName,
    String style,
    String color) {

  public static ZoneTitleDefResDto from(ZoneTitleDef def) {
    return new ZoneTitleDefResDto(
        def.getId().toString(),
        def.getZoneId(),
        def.getTier(),
        def.getRequiredSuccessCount(),
        def.getTitleCode(),
        def.getTitleName(),
        def.getStyle(),
        def.getColor());
  }
}

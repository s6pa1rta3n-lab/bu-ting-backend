package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.ZoneTitleDef;

/** Response DTO representing an equipped title and display metadata. */
public record EquippedTitleResDto(
    String titleCode, String titleName, String style, String color, String zoneId, Integer tier) {

  public static EquippedTitleResDto from(ZoneTitleDef def) {
    if (def == null) return null;
    return new EquippedTitleResDto(
        def.getTitleCode(),
        def.getTitleName(),
        def.getStyle(),
        def.getColor(),
        def.getZoneId(),
        def.getTier());
  }
}

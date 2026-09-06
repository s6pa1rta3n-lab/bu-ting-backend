package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.CityGrade;
import java.util.List;

/** Complete summary of user's equipped title, city grade, earned titles, and progression. */
public record UserTitlesSummaryResDto(
    EquippedTitleResDto equippedTitle,
    CityGrade cityGrade,
    List<UserZoneTitleResDto> earnedTitles,
    List<ZoneProgressResDto> zoneProgressList) {}

package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;

/** Recommendations wrapper for automatic round slot assignment. */
public record AutoAssignRecommendationResDto(List<ZoneRecommendationResDto> recommendations) {}

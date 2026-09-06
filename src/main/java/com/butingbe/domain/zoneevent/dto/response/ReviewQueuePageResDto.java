package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;

/** Pagination response wrapper for review queue items. */
public record ReviewQueuePageResDto(
    List<ReviewQueueItemResDto> items, int totalCount, boolean hasNext) {}

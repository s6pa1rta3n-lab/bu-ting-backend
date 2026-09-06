package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;

/** Keyset pagination wrapper for album feed. */
public record AlbumPageResDto(List<AlbumItemResDto> items, String nextCursor, boolean hasNext) {}

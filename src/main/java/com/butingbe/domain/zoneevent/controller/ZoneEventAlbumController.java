package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.request.AlbumSort;
import com.butingbe.domain.zoneevent.dto.response.AlbumPageResDto;
import com.butingbe.domain.zoneevent.service.ZoneEventAlbumService;
import com.butingbe.global.common.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public album feed endpoints for events, zones, and rounds. */
@RestController
@RequestMapping("/zone-events")
@RequiredArgsConstructor
public class ZoneEventAlbumController {

  private final ZoneEventAlbumService albumService;

  @GetMapping("/{eventId}/album")
  public ResponseEntity<ApiResponse<AlbumPageResDto>> getEventAlbum(
      @PathVariable UUID eventId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "LATEST") AlbumSort sort,
      @AuthenticationPrincipal AuthenticatedUser user) {
    UUID userId = user != null ? user.getUserId() : null;
    return ResponseEntity.ok(
        ApiResponse.success(albumService.getEventAlbum(eventId, cursor, size, sort, userId)));
  }

  @GetMapping("/zones/{zoneId}/album")
  public ResponseEntity<ApiResponse<AlbumPageResDto>> getZoneAlbum(
      @PathVariable String zoneId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "LATEST") AlbumSort sort,
      @AuthenticationPrincipal AuthenticatedUser user) {
    UUID userId = user != null ? user.getUserId() : null;
    return ResponseEntity.ok(
        ApiResponse.success(albumService.getZoneAlbum(zoneId, cursor, size, sort, userId)));
  }

  @GetMapping("/rounds/{roundId}/album")
  public ResponseEntity<ApiResponse<AlbumPageResDto>> getRoundAlbum(
      @PathVariable UUID roundId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "LATEST") AlbumSort sort,
      @AuthenticationPrincipal AuthenticatedUser user) {
    UUID userId = user != null ? user.getUserId() : null;
    return ResponseEntity.ok(
        ApiResponse.success(albumService.getRoundAlbum(roundId, cursor, size, sort, userId)));
  }
}

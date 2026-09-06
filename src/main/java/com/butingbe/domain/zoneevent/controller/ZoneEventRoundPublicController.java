package com.butingbe.domain.zoneevent.controller;

import com.butingbe.domain.zoneevent.dto.response.PublicCurrentRoundResDto;
import com.butingbe.domain.zoneevent.service.AdminZoneEventRoundService;
import com.butingbe.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoint for current active round discovery on mobile apps. */
@RestController
@RequestMapping("/zone-events/rounds")
@RequiredArgsConstructor
public class ZoneEventRoundPublicController {

  private final AdminZoneEventRoundService roundService;

  @GetMapping("/current")
  public ResponseEntity<ApiResponse<PublicCurrentRoundResDto>> getCurrentRound() {
    return ResponseEntity.ok(ApiResponse.success(roundService.getCurrentActiveRound()));
  }
}

package com.butingbe.domain.zoneevent.dto.request;

import com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 이벤트 인증 타겟 요청 DTO.
 *
 * @param targetKind 타겟 구분 (PLACE/OBJECT)
 * @param landmarkId 랜드마크 ID
 * @param placeName 장소 또는 사물명
 * @param guideText 인증 안내문
 * @param exampleFileKey 예시 이미지 파일 키
 * @param latitude 중심 위도 (-90 ~ 90)
 * @param longitude 중심 경도 (-180 ~ 180)
 * @param radiusM 인증 허용 반경 (30m ~ 500m)
 */
public record AdminZoneEventTargetReqDto(
    @NotNull ZoneEventTargetKind targetKind,
    String landmarkId,
    @NotBlank String placeName,
    String guideText,
    String exampleFileKey,
    @NotNull Double latitude,
    @NotNull Double longitude,
    @NotNull @Min(30) @Max(500) Integer radiusM) {}

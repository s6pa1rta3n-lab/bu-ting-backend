package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * 관리자 구역 이벤트 생성 요청 DTO.
 *
 * @param zoneId 구역 식별자 (ChatZone 유효 문자열)
 * @param typeCode 이벤트 유형 코드
 * @param title 이벤트 제목
 * @param description 이벤트 설명
 * @param startsAt 시작 일시
 * @param durationMinutes 진행 시간(분)
 * @param target 인증 대상 장소/사물 정보
 * @param baseReward 기본 보상 설정
 * @param excellenceReward 우수 인증 보상 설정
 * @param successLimitPerUser 유저당 성공 참여 상한
 */
public record AdminZoneEventCreateReqDto(
    @NotBlank String zoneId,
    @NotBlank String typeCode,
    @NotBlank String title,
    String description,
    @NotNull OffsetDateTime startsAt,
    @NotNull @Min(1) Integer durationMinutes,
    @NotNull @Valid AdminZoneEventTargetReqDto target,
    @NotNull @Valid RewardSnapshotReqDto baseReward,
    @Valid RewardSnapshotReqDto excellenceReward,
    @Min(1) Integer successLimitPerUser) {}

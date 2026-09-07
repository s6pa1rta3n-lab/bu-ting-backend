package com.butingbe.domain.zoneevent.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 이벤트별 Top N 및 경계 동점 후보 상세 응답.
 *
 * @param eventId 이벤트 식별자
 * @param zoneId 구역 식별자
 * @param slotCode 슬롯 코드
 * @param title 이벤트 제목
 * @param topN 목표 수상 인원수
 * @param version 스냅샷 버전
 * @param closedAt 회차 마감 시점
 * @param hasTiedBoundary 경계 동점 존재 여부
 * @param finalized 관리자 확정 완료 여부
 * @param snapshotId 스냅샷 식별자
 * @param candidates 후보 목록
 */
public record EventTopNResDto(
    UUID eventId,
    String zoneId,
    String slotCode,
    String title,
    Integer topN,
    Integer version,
    OffsetDateTime closedAt,
    boolean hasTiedBoundary,
    boolean finalized,
    UUID snapshotId,
    List<TopNCandidateResDto> candidates) {}

package com.butingbe.domain.zoneevent.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 수상자 최종 확정 결과 응답.
 *
 * @param eventId 이벤트 식별자
 * @param version 스냅샷 버전
 * @param finalizedParticipationIds 확정된 참여 식별자 목록
 * @param selectionReason 선정 사유
 * @param revision 이벤트 갱신 revision
 */
public record ConfirmWinnersResDto(
    UUID eventId,
    Integer version,
    List<UUID> finalizedParticipationIds,
    String selectionReason,
    Long revision) {}

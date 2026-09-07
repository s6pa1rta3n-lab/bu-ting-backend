package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Top N 경계 동점자 관리자 최종 확정 요청.
 *
 * @param snapshotId 순위 스냅샷 식별자
 * @param participationIds 최종 선정한 참여 식별자 목록
 * @param selectionReason 운영자 선정 사유
 * @param expectedRevision 낙관적 락을 위한 이벤트 기대 revision
 */
public record ConfirmWinnersReqDto(
    @NotNull UUID snapshotId,
    List<UUID> participationIds,
    @NotBlank String selectionReason,
    Long expectedRevision) {}

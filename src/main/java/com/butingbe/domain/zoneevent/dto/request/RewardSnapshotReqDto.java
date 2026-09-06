package com.butingbe.domain.zoneevent.dto.request;

import com.butingbe.domain.zoneevent.entity.RewardSnapshot;

/**
 * 보상 스냅샷 요청 DTO.
 *
 * @param point 지급 포인트
 * @param badge 지급 배지 코드
 * @param chancePercent 지급 확률 (백분율)
 * @param catalogCode 카탈로그 코드
 */
public record RewardSnapshotReqDto(
    Integer point, String badge, Integer chancePercent, String catalogCode) {

  public RewardSnapshot toSnapshot() {
    return new RewardSnapshot(point, badge, chancePercent, catalogCode);
  }
}

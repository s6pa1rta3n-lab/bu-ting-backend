package com.butingbe.domain.reward.dto.response;

import java.util.List;

/**
 * 보상 발급 이력 커서 페이징 응답 DTO.
 *
 * @param items 보상 발급 내역 목록
 * @param nextCursor 다음 커서 문자열
 * @param hasNext 다음 페이지 존재 여부
 */
public record RewardGrantPageResDto(
    List<GrantedRewardDto> items, String nextCursor, boolean hasNext) {}

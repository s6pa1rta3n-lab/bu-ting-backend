package com.butingbe.domain.reward.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

/** Paginated response DTO for reward payout queries. */
public record RewardPayoutPageResDto(
    List<RewardPayoutSummaryResDto> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext) {
  public static RewardPayoutPageResDto from(Page<RewardPayoutSummaryResDto> pageResult) {
    return new RewardPayoutPageResDto(
        pageResult.getContent(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements(),
        pageResult.getTotalPages(),
        pageResult.hasNext());
  }
}

package com.butingbe.domain.reward.dto.response;

import java.util.List;
import java.util.UUID;

/** TOP_LIKE 우수 보상 정산 결과 리포트. */
public record TopLikeSettlementReportResDto(
    UUID targetId,
    int totalCandidates,
    int totalGranted,
    int totalSkippedStock,
    int totalSkippedMonthlyCap,
    List<TopLikeSettlementItemResDto> items) {}

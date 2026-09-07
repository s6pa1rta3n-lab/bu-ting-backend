package com.butingbe.domain.reward.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Request DTO for bulk confirming reward payouts with optimistic concurrency control. */
public record AdminRewardPayoutBulkConfirmReqDto(
    @NotEmpty List<UUID> payoutIds, Map<UUID, Long> expectedRevisions) {}

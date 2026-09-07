package com.butingbe.domain.reward.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Request DTO for bulk scheduling reward payout processing dates. */
public record AdminRewardPayoutBulkScheduleReqDto(
    @NotEmpty List<UUID> payoutIds,
    @NotNull OffsetDateTime scheduledAt,
    Map<UUID, Long> expectedRevisions) {}

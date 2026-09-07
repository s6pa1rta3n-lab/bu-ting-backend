package com.butingbe.domain.reward.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Request DTO for recording collection of shipping information for prizes. */
public record AdminRewardPayoutMarkInfoCollectedReqDto(
    @NotEmpty List<UUID> payoutIds,
    OffsetDateTime informationCollectedAt,
    String note,
    Map<UUID, Long> expectedRevisions) {}

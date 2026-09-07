package com.butingbe.domain.reward.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Request DTO for recording winner notification email dispatch. */
public record AdminRewardPayoutMarkMailSentReqDto(
    @NotEmpty List<UUID> payoutIds,
    OffsetDateTime mailedAt,
    String note,
    Map<UUID, Long> expectedRevisions) {}

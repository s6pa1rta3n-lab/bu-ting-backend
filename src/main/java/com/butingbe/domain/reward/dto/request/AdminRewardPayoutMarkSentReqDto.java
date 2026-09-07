package com.butingbe.domain.reward.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Request DTO for recording actual delivery/shipment completion for prizes. */
public record AdminRewardPayoutMarkSentReqDto(
    @NotEmpty List<UUID> payoutIds,
    OffsetDateTime sentAt,
    String reference,
    String note,
    Map<UUID, Long> expectedRevisions) {}

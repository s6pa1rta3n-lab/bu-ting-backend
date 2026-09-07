package com.butingbe.domain.reward.exception;

import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Exception thrown when a conflict or validation failure occurs during reward payout operations.
 */
@Getter
public class RewardPayoutConflictException extends RuntimeException {

  private final List<UUID> problematicPayoutIds;

  public RewardPayoutConflictException(String message, List<UUID> problematicPayoutIds) {
    super(message);
    this.problematicPayoutIds =
        problematicPayoutIds == null ? List.of() : List.copyOf(problematicPayoutIds);
  }
}

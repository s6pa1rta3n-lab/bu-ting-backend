package com.butingbe.domain.reward.entity;

/** Historical transition action recorded on reward payout lifecycle changes. */
public enum RewardPayoutAction {
  CREATE,
  UPDATE_CONFIG,
  SCHEDULE,
  CONFIRM,
  EXECUTE_PAY,
  MARK_MAIL_SENT,
  MARK_INFO_COLLECTED,
  MARK_SENT,
  RETRY,
  HOLD,
  RELEASE_HOLD
}

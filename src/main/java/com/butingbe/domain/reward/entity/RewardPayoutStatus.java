package com.butingbe.domain.reward.entity;

/**
 * Status lifecycle for reward payouts. BASE: PENDING_CONFIRM -> CONFIRMED -> PAID (or FAILED).
 * TOP_LIKE: PENDING_ASSIGN -> PENDING_CONFIRM -> CONFIRMED -> MAIL_SENT -> INFO_COLLECTED -> SENT.
 */
public enum RewardPayoutStatus {
  PENDING_ASSIGN,
  PENDING_CONFIRM,
  CONFIRMED,
  PAID,
  FAILED,
  MAIL_SENT,
  INFO_COLLECTED,
  SENT
}

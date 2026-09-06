package com.butingbe.domain.reward.dto.response;

/** 우수 보상 정산 항목별 결과 상태. */
public enum SettlementItemStatus {
  GRANTED,
  ALREADY_GRANTED,
  SKIPPED_OUT_OF_STOCK,
  SKIPPED_MONTHLY_CAP,
  SKIPPED_NO_REWARD
}

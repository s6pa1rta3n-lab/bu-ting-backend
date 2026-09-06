package com.butingbe.domain.reward.dto.response;

import java.time.OffsetDateTime;

/**
 * 포인트 원장 내역 단건 응답.
 *
 * @param ledgerId 원장 식별자
 * @param amount 포인트 변동 금액
 * @param reason 변동 사유
 * @param grantId 연관된 지급 식별자
 * @param createdAt 발생 일시
 */
public record PointLedgerEntryResDto(
    String ledgerId, int amount, String reason, String grantId, OffsetDateTime createdAt) {}

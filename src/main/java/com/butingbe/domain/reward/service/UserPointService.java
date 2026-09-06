package com.butingbe.domain.reward.service;

import com.butingbe.domain.reward.entity.UserPointBalance;
import com.butingbe.domain.reward.entity.UserPointLedger;
import com.butingbe.domain.reward.repository.UserPointBalanceRepository;
import com.butingbe.domain.reward.repository.UserPointLedgerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 포인트를 원장과 잔액 캐시로 관리하는 서비스. */
@Service
@RequiredArgsConstructor
public class UserPointService {

  private final UserPointLedgerRepository ledgerRepository;
  private final UserPointBalanceRepository balanceRepository;

  /**
   * 포인트를 증감하고 원장과 잔액 캐시를 함께 갱신한다.
   *
   * @param userId 유저 식별자
   * @param amount 증감할 포인트 수량
   * @param reason 지급 또는 차감 사유
   * @param grantId 연계된 보상 지급 식별자
   * @return 갱신된 포인트 잔액
   */
  @Transactional
  public int record(UUID userId, int amount, String reason, UUID grantId) {
    ledgerRepository.save(
        UserPointLedger.builder()
            .userId(userId)
            .amount(amount)
            .reason(reason)
            .grantId(grantId)
            .build());

    UserPointBalance balance =
        balanceRepository
            .findById(userId)
            .orElseGet(() -> UserPointBalance.builder().userId(userId).balance(0).build());
    balance.add(amount);
    return balanceRepository.save(balance).getBalance();
  }

  /**
   * 유저의 현재 포인트 잔액을 조회한다.
   *
   * @param userId 유저 식별자
   * @return 현재 잔액 (원장이 없으면 0)
   */
  @Transactional(readOnly = true)
  public int getBalance(UUID userId) {
    return balanceRepository.findById(userId).map(UserPointBalance::getBalance).orElse(0);
  }
}

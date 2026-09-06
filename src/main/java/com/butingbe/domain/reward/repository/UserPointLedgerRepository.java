package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.UserPointLedger;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저 포인트 증감 원장 저장소 인터페이스. */
public interface UserPointLedgerRepository extends JpaRepository<UserPointLedger, UUID> {

  /**
   * 유저의 모든 포인트 원장 합계를 계산한다.
   *
   * @param userId 유저 식별자
   * @return 원장 증감액 총합
   */
  @Query("SELECT COALESCE(SUM(l.amount), 0) FROM UserPointLedger l WHERE l.userId = :userId")
  long sumAmountByUserId(@Param("userId") UUID userId);
}

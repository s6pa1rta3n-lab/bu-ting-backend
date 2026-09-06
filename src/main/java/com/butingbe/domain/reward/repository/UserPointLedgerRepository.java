package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.UserPointLedger;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저 포인트 원장 저장소. */
public interface UserPointLedgerRepository
    extends JpaRepository<UserPointLedger, UUID>, JpaSpecificationExecutor<UserPointLedger> {

  /**
   * 유저의 포인트 원장 총합을 조회한다.
   *
   * @param userId 유저 식별자
   * @return 포인트 원장 총합
   */
  @Query("SELECT COALESCE(SUM(l.amount), 0) FROM UserPointLedger l WHERE l.userId = :userId")
  long sumAmountByUserId(@Param("userId") UUID userId);
}

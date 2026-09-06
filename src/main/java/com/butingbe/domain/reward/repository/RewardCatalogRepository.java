package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.RewardCatalog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 보상 카탈로그 저장소 인터페이스. */
public interface RewardCatalogRepository extends JpaRepository<RewardCatalog, UUID> {

  /**
   * 보상 코드로 카탈로그를 조회한다.
   *
   * @param code 보상 식별 코드
   * @return 일치하는 보상 카탈로그 엔티티
   */
  Optional<RewardCatalog> findByCode(String code);

  /**
   * 해당 코드의 보상 카탈로그가 존재하는지 확인한다.
   *
   * @param code 보상 식별 코드
   * @return 존재 여부
   */
  boolean existsByCode(String code);
}

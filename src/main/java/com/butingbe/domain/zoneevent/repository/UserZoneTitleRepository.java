package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.UserZoneTitle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for user-earned zone titles. */
public interface UserZoneTitleRepository extends JpaRepository<UserZoneTitle, UUID> {

  /** Finds all titles earned by a user. */
  List<UserZoneTitle> findByUserId(UUID userId);

  /** Finds the equipped title for a user if any. */
  Optional<UserZoneTitle> findByUserIdAndIsEquippedTrue(UUID userId);

  /** Finds a specific earned title by user ID and title definition ID. */
  @Query(
      "SELECT uzt FROM UserZoneTitle uzt WHERE uzt.userId = :userId AND uzt.titleDef.id = :titleId")
  Optional<UserZoneTitle> findByUserIdAndTitleDefId(
      @Param("userId") UUID userId, @Param("titleId") UUID titleId);

  /** Checks if a user has earned a specific title definition. */
  @Query(
      "SELECT COUNT(uzt) > 0 FROM UserZoneTitle uzt WHERE uzt.userId = :userId AND uzt.titleDef.id = :titleId")
  boolean existsByUserIdAndTitleDefId(@Param("userId") UUID userId, @Param("titleId") UUID titleId);

  /** Checks if user currently has an equipped title. */
  boolean existsByUserIdAndIsEquippedTrue(UUID userId);

  /** Counts total titles earned by user. */
  long countByUserId(UUID userId);
}

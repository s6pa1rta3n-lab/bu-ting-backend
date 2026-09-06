package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.CityGrade;
import com.butingbe.domain.zoneevent.entity.UserCityGradeHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for user city grade achievement history. */
public interface UserCityGradeHistoryRepository extends JpaRepository<UserCityGradeHistory, UUID> {

  /** Finds full city grade history for a user in reverse chronological order. */
  List<UserCityGradeHistory> findByUserIdOrderByReachedAtDesc(UUID userId);

  /** Finds highest/latest achieved city grade entry for a user. */
  Optional<UserCityGradeHistory> findFirstByUserIdOrderByReachedAtDesc(UUID userId);

  /** Checks if user has already achieved a specific city grade. */
  boolean existsByUserIdAndGrade(UUID userId, CityGrade grade);
}

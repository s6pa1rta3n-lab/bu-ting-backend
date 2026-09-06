package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.ZoneEventLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for managing zone event likes. */
public interface ZoneEventLikeRepository extends JpaRepository<ZoneEventLike, UUID> {

  /** Checks if a user has liked a specific participation. */
  boolean existsByParticipationIdAndUserId(UUID participationId, UUID userId);

  /** Finds a specific like by participation ID and user ID. */
  Optional<ZoneEventLike> findByParticipationIdAndUserId(UUID participationId, UUID userId);

  /** Deletes a like by participation ID and user ID. */
  void deleteByParticipationIdAndUserId(UUID participationId, UUID userId);

  /** Counts likes for a given participation. */
  long countByParticipationId(UUID participationId);
}

package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.ZoneEventComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for managing zone event comments. */
public interface ZoneEventCommentRepository extends JpaRepository<ZoneEventComment, UUID> {

  /** Finds all non-deleted comments for a participation in chronological order. */
  List<ZoneEventComment> findByParticipationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
      UUID participationId);

  /** Counts active non-deleted comments for a participation. */
  long countByParticipationIdAndDeletedAtIsNull(UUID participationId);
}

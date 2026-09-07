package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ZoneEventReportRepository extends JpaRepository<ZoneEventReport, UUID> {

  boolean existsByParticipationIdAndReporterId(UUID participationId, UUID reporterId);

  long countByParticipationId(UUID participationId);

  List<ZoneEventReport> findByParticipationId(UUID participationId);

  boolean existsByParticipationIdAndStatusIn(
      UUID participationId, Collection<ReportStatus> statuses);

  long countByParticipationIdAndStatusIn(UUID participationId, Collection<ReportStatus> statuses);

  @Query(
      value =
          """
          SELECT r FROM ZoneEventReport r
          WHERE (:status IS NULL OR r.status = :status)
            AND (:participationId IS NULL OR r.participationId = :participationId)
            AND (:eventId IS NULL OR r.participationId IN (
                SELECT p.id FROM ZoneEventParticipation p WHERE p.event.id = :eventId
            ))
            AND (:roundId IS NULL OR r.participationId IN (
                SELECT p2.id FROM ZoneEventParticipation p2 WHERE p2.event.roundId = :roundId
            ))
          ORDER BY r.createdAt DESC
          """,
      countQuery =
          """
          SELECT COUNT(r) FROM ZoneEventReport r
          WHERE (:status IS NULL OR r.status = :status)
            AND (:participationId IS NULL OR r.participationId = :participationId)
            AND (:eventId IS NULL OR r.participationId IN (
                SELECT p.id FROM ZoneEventParticipation p WHERE p.event.id = :eventId
            ))
            AND (:roundId IS NULL OR r.participationId IN (
                SELECT p2.id FROM ZoneEventParticipation p2 WHERE p2.event.roundId = :roundId
            ))
          """)
  Page<ZoneEventReport> searchReports(
      @Param("status") ReportStatus status,
      @Param("roundId") UUID roundId,
      @Param("eventId") UUID eventId,
      @Param("participationId") UUID participationId,
      Pageable pageable);
}

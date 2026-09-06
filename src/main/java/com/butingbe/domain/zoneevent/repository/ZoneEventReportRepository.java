package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for managing zone event reports. */
public interface ZoneEventReportRepository extends JpaRepository<ZoneEventReport, UUID> {

  /** Checks whether a user has already reported a participation. */
  boolean existsByParticipationIdAndReporterId(UUID participationId, UUID reporterId);

  /** Counts active reports for a participation by status. */
  long countByParticipationIdAndStatus(UUID participationId, ReportStatus status);

  /** Finds all reports filed against a participation. */
  List<ZoneEventReport> findByParticipationId(UUID participationId);

  /** Finds all reports filed against a participation with a given status. */
  List<ZoneEventReport> findByParticipationIdAndStatus(UUID participationId, ReportStatus status);
}

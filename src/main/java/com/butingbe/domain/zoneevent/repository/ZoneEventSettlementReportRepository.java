package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.ZoneEventSettlementReport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for round settlement snapshot reports. */
public interface ZoneEventSettlementReportRepository
    extends JpaRepository<ZoneEventSettlementReport, UUID> {

  /** Finds settlement report by round ID. */
  Optional<ZoneEventSettlementReport> findByRoundId(UUID roundId);
}

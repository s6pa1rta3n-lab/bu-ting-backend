package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.ZoneTitleDef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for zone title definitions. */
public interface ZoneTitleDefRepository extends JpaRepository<ZoneTitleDef, UUID> {

  /** Finds title definitions for a specific zone ordered by tier. */
  List<ZoneTitleDef> findByZoneIdOrderByTierAsc(String zoneId);

  /** Finds title definition by zone ID and tier. */
  Optional<ZoneTitleDef> findByZoneIdAndTier(String zoneId, Integer tier);

  /** Finds title definition by title code. */
  Optional<ZoneTitleDef> findByTitleCode(String titleCode);

  /** Finds all title definitions ordered by zone and tier. */
  List<ZoneTitleDef> findAllByOrderByZoneIdAscTierAsc();
}

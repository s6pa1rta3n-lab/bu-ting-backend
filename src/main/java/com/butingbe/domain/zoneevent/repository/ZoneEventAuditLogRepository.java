package com.butingbe.domain.zoneevent.repository;

import com.butingbe.domain.zoneevent.entity.ZoneEventAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for zone event audit logs. */
public interface ZoneEventAuditLogRepository extends JpaRepository<ZoneEventAuditLog, UUID> {

  /** Finds audit logs by target type and target ID. */
  List<ZoneEventAuditLog> findByTargetTypeAndTargetId(String targetType, String targetId);
}

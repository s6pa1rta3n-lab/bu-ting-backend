package com.butingbe.domain.notification.repository;

import com.butingbe.domain.notification.entity.PushNotificationLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for push notification delivery audit logs. */
public interface PushNotificationLogRepository extends JpaRepository<PushNotificationLog, UUID> {

  /** Finds notification logs sent to a specific user ordered by sent time descending. */
  List<PushNotificationLog> findByUserIdOrderBySentAtDesc(UUID userId);

  /** Finds most recent dispatched notification logs. */
  List<PushNotificationLog> findTop20ByOrderBySentAtDesc();
}

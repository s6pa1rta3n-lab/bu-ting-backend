package com.butingbe.domain.notification.repository;

import com.butingbe.domain.notification.entity.UserZoneSubscription;
import com.butingbe.domain.notification.entity.UserZoneSubscriptionId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for user zone push subscriptions. */
public interface UserZoneSubscriptionRepository
    extends JpaRepository<UserZoneSubscription, UserZoneSubscriptionId> {

  /** Finds all zone subscriptions for a given user. */
  List<UserZoneSubscription> findByIdUserId(UUID userId);

  /** Finds all active zone subscriptions for a given user. */
  List<UserZoneSubscription> findByIdUserIdAndIsActiveTrue(UUID userId);

  /** Finds all active subscribers for a specific zone. */
  List<UserZoneSubscription> findByIdZoneIdAndIsActiveTrue(String zoneId);

  /** Finds subscription for a specific user and zone. */
  Optional<UserZoneSubscription> findByIdUserIdAndIdZoneId(UUID userId, String zoneId);
}

package com.butingbe.domain.notification.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing a user subscription to zone event notifications for a specific zone. */
@Entity
@Table(name = "user_zone_subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserZoneSubscription extends TimestampEntity {

  @EmbeddedId private UserZoneSubscriptionId id;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @Builder
  private UserZoneSubscription(UUID userId, String zoneId, Boolean isActive) {
    this.id = new UserZoneSubscriptionId(userId, zoneId);
    this.isActive = isActive == null || isActive;
  }

  /** Activates the subscription. */
  public void activate() {
    this.isActive = true;
  }

  /** Deactivates the subscription. */
  public void deactivate() {
    this.isActive = false;
  }
}

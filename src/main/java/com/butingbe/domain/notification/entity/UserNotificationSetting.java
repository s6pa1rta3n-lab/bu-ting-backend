package com.butingbe.domain.notification.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing user preference toggles for notifications. */
@Entity
@Table(name = "user_notification_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationSetting extends TimestampEntity {

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "push_enabled", nullable = false)
  private Boolean pushEnabled;

  @Column(name = "zone_event_enabled", nullable = false)
  private Boolean zoneEventEnabled;

  @Column(name = "settlement_enabled", nullable = false)
  private Boolean settlementEnabled;

  @Builder
  private UserNotificationSetting(
      UUID userId, Boolean pushEnabled, Boolean zoneEventEnabled, Boolean settlementEnabled) {
    this.userId = userId;
    this.pushEnabled = pushEnabled == null || pushEnabled;
    this.zoneEventEnabled = zoneEventEnabled == null || zoneEventEnabled;
    this.settlementEnabled = settlementEnabled == null || settlementEnabled;
  }

  /**
   * Updates notification setting flags.
   *
   * @param pushEnabled global push toggle
   * @param zoneEventEnabled zone event push toggle
   * @param settlementEnabled settlement push toggle
   */
  public void updateSettings(
      Boolean pushEnabled, Boolean zoneEventEnabled, Boolean settlementEnabled) {
    if (pushEnabled != null) this.pushEnabled = pushEnabled;
    if (zoneEventEnabled != null) this.zoneEventEnabled = zoneEventEnabled;
    if (settlementEnabled != null) this.settlementEnabled = settlementEnabled;
  }
}

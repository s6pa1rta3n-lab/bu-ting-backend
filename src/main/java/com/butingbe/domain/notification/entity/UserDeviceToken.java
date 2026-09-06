package com.butingbe.domain.notification.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing a registered user device push token. */
@Entity
@Table(name = "user_device_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeviceToken extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "token_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "fcm_token", nullable = false, unique = true, length = 512)
  private String fcmToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "device_type", nullable = false, length = 20)
  private DeviceType deviceType;

  @Column(name = "last_seen_at", nullable = false)
  private OffsetDateTime lastSeenAt;

  @Builder
  private UserDeviceToken(
      UUID userId, String fcmToken, DeviceType deviceType, OffsetDateTime lastSeenAt) {
    this.userId = userId;
    this.fcmToken = fcmToken;
    this.deviceType = deviceType == null ? DeviceType.ANDROID : deviceType;
    this.lastSeenAt = lastSeenAt == null ? OffsetDateTime.now() : lastSeenAt;
  }

  /** Updates the last seen timestamp of this device token. */
  public void updateLastSeen() {
    this.lastSeenAt = OffsetDateTime.now();
  }
}

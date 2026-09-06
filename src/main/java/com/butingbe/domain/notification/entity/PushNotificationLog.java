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

/** Audit log entity for dispatched push notifications. */
@Entity
@Table(name = "push_notification_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushNotificationLog extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "log_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id")
  private UUID userId;

  @Column(length = 100)
  private String topic;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PushNotificationStatus status;

  @Column(name = "sent_at", nullable = false)
  private OffsetDateTime sentAt;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @Builder
  private PushNotificationLog(
      UUID userId,
      String topic,
      String title,
      String body,
      PushNotificationStatus status,
      OffsetDateTime sentAt,
      String errorMessage) {
    this.userId = userId;
    this.topic = topic;
    this.title = title;
    this.body = body;
    this.status = status == null ? PushNotificationStatus.SENT : status;
    this.sentAt = sentAt == null ? OffsetDateTime.now() : sentAt;
    this.errorMessage = errorMessage;
  }
}

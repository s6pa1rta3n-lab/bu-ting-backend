package com.butingbe.domain.notification.service;

import com.butingbe.domain.notification.dto.response.NotificationSettingResDto;
import com.butingbe.domain.notification.dto.response.ZoneSubscriptionResDto;
import com.butingbe.domain.notification.entity.DeviceType;
import com.butingbe.domain.notification.entity.PushNotificationLog;
import com.butingbe.domain.notification.entity.PushNotificationStatus;
import com.butingbe.domain.notification.entity.UserDeviceToken;
import com.butingbe.domain.notification.entity.UserNotificationSetting;
import com.butingbe.domain.notification.entity.UserZoneSubscription;
import com.butingbe.domain.notification.repository.PushNotificationLogRepository;
import com.butingbe.domain.notification.repository.UserDeviceTokenRepository;
import com.butingbe.domain.notification.repository.UserNotificationSettingRepository;
import com.butingbe.domain.notification.repository.UserZoneSubscriptionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service managing user push devices, subscriptions, notification settings, and delivery. */
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final UserDeviceTokenRepository deviceTokenRepository;
  private final UserZoneSubscriptionRepository zoneSubscriptionRepository;
  private final UserNotificationSettingRepository notificationSettingRepository;
  private final PushNotificationLogRepository pushNotificationLogRepository;
  private final FcmPushService fcmPushService;

  /** Registers or refreshes an FCM device token for a user. */
  @Transactional
  public void registerDeviceToken(UUID userId, String fcmToken, DeviceType deviceType) {
    deviceTokenRepository
        .findByFcmToken(fcmToken)
        .ifPresentOrElse(
            UserDeviceToken::updateLastSeen,
            () ->
                deviceTokenRepository.save(
                    UserDeviceToken.builder()
                        .userId(userId)
                        .fcmToken(fcmToken)
                        .deviceType(deviceType)
                        .lastSeenAt(OffsetDateTime.now())
                        .build()));
  }

  /** Unregisters an FCM device token. */
  @Transactional
  public void unregisterDeviceToken(UUID userId, String fcmToken) {
    deviceTokenRepository.deleteByUserIdAndFcmToken(userId, fcmToken);
  }

  /** Subscribes user to notifications for a specific zone. */
  @Transactional
  public void subscribeZone(UUID userId, String zoneId) {
    zoneSubscriptionRepository
        .findByIdUserIdAndIdZoneId(userId, zoneId)
        .ifPresentOrElse(
            UserZoneSubscription::activate,
            () ->
                zoneSubscriptionRepository.save(
                    UserZoneSubscription.builder()
                        .userId(userId)
                        .zoneId(zoneId)
                        .isActive(true)
                        .build()));
  }

  /** Unsubscribes user from notifications for a specific zone. */
  @Transactional
  public void unsubscribeZone(UUID userId, String zoneId) {
    zoneSubscriptionRepository
        .findByIdUserIdAndIdZoneId(userId, zoneId)
        .ifPresent(UserZoneSubscription::deactivate);
  }

  /** Retrieves all active zone subscriptions for a user. */
  @Transactional(readOnly = true)
  public List<ZoneSubscriptionResDto> getUserSubscriptions(UUID userId) {
    return zoneSubscriptionRepository.findByIdUserId(userId).stream()
        .map(sub -> new ZoneSubscriptionResDto(sub.getId().getZoneId(), sub.getIsActive()))
        .collect(Collectors.toList());
  }

  /** Updates user notification preference settings. */
  @Transactional
  public NotificationSettingResDto updateNotificationSettings(
      UUID userId, Boolean pushEnabled, Boolean zoneEventEnabled, Boolean settlementEnabled) {
    UserNotificationSetting setting =
        notificationSettingRepository
            .findById(userId)
            .orElseGet(
                () ->
                    notificationSettingRepository.save(
                        UserNotificationSetting.builder().userId(userId).build()));

    setting.updateSettings(pushEnabled, zoneEventEnabled, settlementEnabled);
    return NotificationSettingResDto.from(setting);
  }

  /** Retrieves user notification preference settings. */
  @Transactional(readOnly = true)
  public NotificationSettingResDto getNotificationSettings(UUID userId) {
    return notificationSettingRepository
        .findById(userId)
        .map(NotificationSettingResDto::from)
        .orElse(new NotificationSettingResDto(true, true, true));
  }

  /**
   * Dispatches push notification to a user across registered devices if preferences permit.
   *
   * @param userId recipient user ID
   * @param title notification title
   * @param body notification body
   * @param category category identifier (e.g. ZONE_EVENT, SETTLEMENT)
   */
  @Transactional
  public void sendToUser(UUID userId, String title, String body, String category) {
    UserNotificationSetting setting =
        notificationSettingRepository
            .findById(userId)
            .orElseGet(() -> UserNotificationSetting.builder().userId(userId).build());

    if (!Boolean.TRUE.equals(setting.getPushEnabled())) {
      return;
    }
    if ("ZONE_EVENT".equalsIgnoreCase(category)
        && !Boolean.TRUE.equals(setting.getZoneEventEnabled())) {
      return;
    }
    if ("SETTLEMENT".equalsIgnoreCase(category)
        && !Boolean.TRUE.equals(setting.getSettlementEnabled())) {
      return;
    }

    List<UserDeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
    for (UserDeviceToken deviceToken : tokens) {
      PushNotificationStatus status =
          fcmPushService.sendPush(
              deviceToken.getFcmToken(), title, body, Map.of("category", category));
      pushNotificationLogRepository.save(
          PushNotificationLog.builder()
              .userId(userId)
              .title(title)
              .body(body)
              .status(status)
              .sentAt(OffsetDateTime.now())
              .build());
    }
  }

  /**
   * Dispatches notification to all active subscribers of a zone.
   *
   * @param zoneId target zone ID
   * @param title notification title
   * @param body notification body
   */
  @Transactional
  public void sendToZoneSubscribers(String zoneId, String title, String body) {
    List<UserZoneSubscription> subscribers =
        zoneSubscriptionRepository.findByIdZoneIdAndIsActiveTrue(zoneId);
    for (UserZoneSubscription sub : subscribers) {
      sendToUser(sub.getId().getUserId(), title, body, "ZONE_EVENT");
    }
  }

  /**
   * Dispatches topic push notification.
   *
   * @param topic target topic
   * @param title notification title
   * @param body notification body
   */
  @Transactional
  public void sendTopic(String topic, String title, String body) {
    PushNotificationStatus status = fcmPushService.sendTopicPush(topic, title, body);
    pushNotificationLogRepository.save(
        PushNotificationLog.builder()
            .topic(topic)
            .title(title)
            .body(body)
            .status(status)
            .sentAt(OffsetDateTime.now())
            .build());
  }
}

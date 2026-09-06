package com.butingbe.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final UUID USER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final String FCM_TOKEN = "fcm_token_sample_12345";
  private static final String ZONE_ID = "GWANGAN";

  @Mock private UserDeviceTokenRepository deviceTokenRepository;
  @Mock private UserZoneSubscriptionRepository zoneSubscriptionRepository;
  @Mock private UserNotificationSettingRepository notificationSettingRepository;
  @Mock private PushNotificationLogRepository pushNotificationLogRepository;
  @Mock private FcmPushService fcmPushService;

  @InjectMocks private NotificationService notificationService;

  @Test
  @DisplayName("registerDeviceToken updates existing token last seen")
  void registerDeviceToken_existing() {
    UserDeviceToken existing =
        UserDeviceToken.builder()
            .userId(USER_ID)
            .fcmToken(FCM_TOKEN)
            .deviceType(DeviceType.ANDROID)
            .lastSeenAt(OffsetDateTime.now().minusDays(1))
            .build();
    when(deviceTokenRepository.findByFcmToken(FCM_TOKEN)).thenReturn(Optional.of(existing));

    notificationService.registerDeviceToken(USER_ID, FCM_TOKEN, DeviceType.ANDROID);

    verify(deviceTokenRepository, never()).save(any());
  }

  @Test
  @DisplayName("registerDeviceToken saves new token when not found")
  void registerDeviceToken_new() {
    when(deviceTokenRepository.findByFcmToken(FCM_TOKEN)).thenReturn(Optional.empty());

    notificationService.registerDeviceToken(USER_ID, FCM_TOKEN, DeviceType.IOS);

    verify(deviceTokenRepository).save(any(UserDeviceToken.class));
  }

  @Test
  @DisplayName("unregisterDeviceToken removes token")
  void unregisterDeviceToken_success() {
    notificationService.unregisterDeviceToken(USER_ID, FCM_TOKEN);
    verify(deviceTokenRepository).deleteByUserIdAndFcmToken(USER_ID, FCM_TOKEN);
  }

  @Test
  @DisplayName("subscribeZone activates subscription if already present")
  void subscribeZone_existing() {
    UserZoneSubscription sub =
        UserZoneSubscription.builder().userId(USER_ID).zoneId(ZONE_ID).isActive(false).build();
    when(zoneSubscriptionRepository.findByIdUserIdAndIdZoneId(USER_ID, ZONE_ID))
        .thenReturn(Optional.of(sub));

    notificationService.subscribeZone(USER_ID, ZONE_ID);

    assertThat(sub.getIsActive()).isTrue();
  }

  @Test
  @DisplayName("subscribeZone creates new active subscription if not present")
  void subscribeZone_new() {
    when(zoneSubscriptionRepository.findByIdUserIdAndIdZoneId(USER_ID, ZONE_ID))
        .thenReturn(Optional.empty());

    notificationService.subscribeZone(USER_ID, ZONE_ID);

    verify(zoneSubscriptionRepository).save(any(UserZoneSubscription.class));
  }

  @Test
  @DisplayName("unsubscribeZone deactivates subscription")
  void unsubscribeZone_success() {
    UserZoneSubscription sub =
        UserZoneSubscription.builder().userId(USER_ID).zoneId(ZONE_ID).isActive(true).build();
    when(zoneSubscriptionRepository.findByIdUserIdAndIdZoneId(USER_ID, ZONE_ID))
        .thenReturn(Optional.of(sub));

    notificationService.unsubscribeZone(USER_ID, ZONE_ID);

    assertThat(sub.getIsActive()).isFalse();
  }

  @Test
  @DisplayName("getUserSubscriptions returns mapped list")
  void getUserSubscriptions_success() {
    UserZoneSubscription sub =
        UserZoneSubscription.builder().userId(USER_ID).zoneId(ZONE_ID).isActive(true).build();
    when(zoneSubscriptionRepository.findByIdUserId(USER_ID)).thenReturn(List.of(sub));

    List<ZoneSubscriptionResDto> list = notificationService.getUserSubscriptions(USER_ID);

    assertThat(list).hasSize(1);
    assertThat(list.get(0).zoneId()).isEqualTo(ZONE_ID);
    assertThat(list.get(0).isActive()).isTrue();
  }

  @Test
  @DisplayName("updateNotificationSettings updates existing settings")
  void updateNotificationSettings_success() {
    UserNotificationSetting setting =
        UserNotificationSetting.builder().userId(USER_ID).pushEnabled(true).build();
    when(notificationSettingRepository.findById(USER_ID)).thenReturn(Optional.of(setting));

    NotificationSettingResDto dto =
        notificationService.updateNotificationSettings(USER_ID, false, false, true);

    assertThat(dto.pushEnabled()).isFalse();
    assertThat(dto.zoneEventEnabled()).isFalse();
    assertThat(dto.settlementEnabled()).isTrue();
  }

  @Test
  @DisplayName("getNotificationSettings returns default when none configured")
  void getNotificationSettings_default() {
    when(notificationSettingRepository.findById(USER_ID)).thenReturn(Optional.empty());

    NotificationSettingResDto dto = notificationService.getNotificationSettings(USER_ID);

    assertThat(dto.pushEnabled()).isTrue();
    assertThat(dto.zoneEventEnabled()).isTrue();
    assertThat(dto.settlementEnabled()).isTrue();
  }

  @Test
  @DisplayName("sendToUser skips dispatch if pushEnabled is false")
  void sendToUser_pushDisabled() {
    UserNotificationSetting setting =
        UserNotificationSetting.builder().userId(USER_ID).pushEnabled(false).build();
    when(notificationSettingRepository.findById(USER_ID)).thenReturn(Optional.of(setting));

    notificationService.sendToUser(USER_ID, "제목", "본문", "ZONE_EVENT");

    verify(fcmPushService, never()).sendPush(any(), any(), any(), any());
  }

  @Test
  @DisplayName("sendToUser skips dispatch if category setting disabled")
  void sendToUser_categoryDisabled() {
    UserNotificationSetting setting =
        UserNotificationSetting.builder()
            .userId(USER_ID)
            .pushEnabled(true)
            .zoneEventEnabled(false)
            .settlementEnabled(false)
            .build();
    when(notificationSettingRepository.findById(USER_ID)).thenReturn(Optional.of(setting));

    notificationService.sendToUser(USER_ID, "제목", "본문", "ZONE_EVENT");
    notificationService.sendToUser(USER_ID, "제목", "본문", "SETTLEMENT");

    verify(fcmPushService, never()).sendPush(any(), any(), any(), any());
  }

  @Test
  @DisplayName("sendToUser dispatches push to user devices and logs")
  void sendToUser_success() {
    UserNotificationSetting setting =
        UserNotificationSetting.builder()
            .userId(USER_ID)
            .pushEnabled(true)
            .zoneEventEnabled(true)
            .settlementEnabled(true)
            .build();
    UserDeviceToken token = UserDeviceToken.builder().userId(USER_ID).fcmToken(FCM_TOKEN).build();

    when(notificationSettingRepository.findById(USER_ID)).thenReturn(Optional.of(setting));
    when(deviceTokenRepository.findByUserId(USER_ID)).thenReturn(List.of(token));
    when(fcmPushService.sendPush(eq(FCM_TOKEN), any(), any(), any()))
        .thenReturn(PushNotificationStatus.SENT);

    notificationService.sendToUser(USER_ID, "제목", "본문", "ZONE_EVENT");

    verify(pushNotificationLogRepository).save(any(PushNotificationLog.class));
  }

  @Test
  @DisplayName("sendToZoneSubscribers delivers to all active zone subscribers")
  void sendToZoneSubscribers_success() {
    UserZoneSubscription sub =
        UserZoneSubscription.builder().userId(USER_ID).zoneId(ZONE_ID).isActive(true).build();
    when(zoneSubscriptionRepository.findByIdZoneIdAndIsActiveTrue(ZONE_ID))
        .thenReturn(List.of(sub));

    notificationService.sendToZoneSubscribers(ZONE_ID, "제목", "본문");

    verify(notificationSettingRepository).findById(USER_ID);
  }

  @Test
  @DisplayName("sendTopic dispatches topic push and logs")
  void sendTopic_success() {
    when(fcmPushService.sendTopicPush("all", "제목", "본문"))
        .thenReturn(PushNotificationStatus.SIMULATED);

    notificationService.sendTopic("all", "제목", "본문");

    verify(pushNotificationLogRepository).save(any(PushNotificationLog.class));
  }
}

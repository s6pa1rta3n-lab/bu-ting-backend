package com.butingbe.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.butingbe.domain.notification.entity.PushNotificationStatus;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FcmPushServiceTest {

  private final FcmPushService fcmPushService = new FcmPushService();
  private final FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
  private final FcmPushService mockFcmPushService = new FcmPushService(firebaseMessaging);

  @Test
  @DisplayName("sendPush returns SIMULATED when FirebaseApp is not configured")
  void sendPush_simulated() {
    PushNotificationStatus status =
        fcmPushService.sendPush("test_token", "제목", "본문", Map.of("key", "value"));
    assertThat(status).isEqualTo(PushNotificationStatus.SIMULATED);
  }

  @Test
  @DisplayName("sendTopicPush returns SIMULATED when FirebaseApp is not configured")
  void sendTopicPush_simulated() {
    PushNotificationStatus status = fcmPushService.sendTopicPush("test_topic", "제목", "본문");
    assertThat(status).isEqualTo(PushNotificationStatus.SIMULATED);
  }

  @Test
  @DisplayName("sendPush returns SENT when FirebaseMessaging succeeds")
  void sendPush_sent() throws Exception {
    when(firebaseMessaging.send(any(Message.class))).thenReturn("msg-1");
    PushNotificationStatus status =
        mockFcmPushService.sendPush("token", "title", "body", Map.of("k", "v"));
    assertThat(status).isEqualTo(PushNotificationStatus.SENT);

    PushNotificationStatus nullDataStatus =
        mockFcmPushService.sendPush("token", "title", "body", null);
    assertThat(nullDataStatus).isEqualTo(PushNotificationStatus.SENT);
  }

  @Test
  @DisplayName("sendPush returns FAILED when FirebaseMessaging throws exception")
  void sendPush_failed() throws Exception {
    when(firebaseMessaging.send(any(Message.class))).thenThrow(new RuntimeException("FCM error"));
    PushNotificationStatus status = mockFcmPushService.sendPush("token", "title", "body", Map.of());
    assertThat(status).isEqualTo(PushNotificationStatus.FAILED);
  }

  @Test
  @DisplayName("sendTopicPush returns SENT when FirebaseMessaging succeeds")
  void sendTopicPush_sent() throws Exception {
    when(firebaseMessaging.send(any(Message.class))).thenReturn("msg-2");
    PushNotificationStatus status = mockFcmPushService.sendTopicPush("topic", "title", "body");
    assertThat(status).isEqualTo(PushNotificationStatus.SENT);
  }

  @Test
  @DisplayName("sendTopicPush returns FAILED when FirebaseMessaging throws exception")
  void sendTopicPush_failed() throws Exception {
    when(firebaseMessaging.send(any(Message.class))).thenThrow(new RuntimeException("FCM error"));
    PushNotificationStatus status = mockFcmPushService.sendTopicPush("topic", "title", "body");
    assertThat(status).isEqualTo(PushNotificationStatus.FAILED);
  }
}

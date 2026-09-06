package com.butingbe.domain.notification.service;

import com.butingbe.domain.notification.entity.PushNotificationStatus;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service encapsulating FCM push delivery via Firebase Admin SDK. */
@Service
@Slf4j
public class FcmPushService {

  private final FirebaseMessaging firebaseMessaging;

  public FcmPushService() {
    this(null);
  }

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  public FcmPushService(FirebaseMessaging firebaseMessaging) {
    this.firebaseMessaging = firebaseMessaging;
  }

  /**
   * Dispatches push notification to a device FCM token.
   *
   * @param token target FCM token
   * @param title notification title
   * @param body notification body
   * @param data additional payload
   * @return delivery status (SENT, SIMULATED, or FAILED)
   */
  public PushNotificationStatus sendPush(
      String token, String title, String body, Map<String, String> data) {
    if (firebaseMessaging == null && FirebaseApp.getApps().isEmpty()) {
      log.debug("FirebaseApp not configured. Simulating push dispatch to token: {}", token);
      return PushNotificationStatus.SIMULATED;
    }

    try {
      Message.Builder builder =
          Message.builder()
              .setToken(token)
              .setNotification(Notification.builder().setTitle(title).setBody(body).build());

      if (data != null && !data.isEmpty()) {
        builder.putAllData(data);
      }

      FirebaseMessaging messaging =
          firebaseMessaging != null ? firebaseMessaging : FirebaseMessaging.getInstance();
      messaging.send(builder.build());
      return PushNotificationStatus.SENT;
    } catch (Exception e) {
      log.error("Failed to dispatch push notification: {}", e.getMessage());
      return PushNotificationStatus.FAILED;
    }
  }

  /**
   * Dispatches push notification to an FCM topic.
   *
   * @param topic topic name
   * @param title notification title
   * @param body notification body
   * @return delivery status
   */
  public PushNotificationStatus sendTopicPush(String topic, String title, String body) {
    if (firebaseMessaging == null && FirebaseApp.getApps().isEmpty()) {
      log.debug("FirebaseApp not configured. Simulating topic push to: {}", topic);
      return PushNotificationStatus.SIMULATED;
    }

    try {
      Message message =
          Message.builder()
              .setTopic(topic)
              .setNotification(Notification.builder().setTitle(title).setBody(body).build())
              .build();

      FirebaseMessaging messaging =
          firebaseMessaging != null ? firebaseMessaging : FirebaseMessaging.getInstance();
      messaging.send(message);
      return PushNotificationStatus.SENT;
    } catch (Exception e) {
      log.error("Failed to dispatch topic push notification: {}", e.getMessage());
      return PushNotificationStatus.FAILED;
    }
  }
}

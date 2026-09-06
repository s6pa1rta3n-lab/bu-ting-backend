package com.butingbe.domain.notification.repository;

import com.butingbe.domain.notification.entity.UserDeviceToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for user FCM device tokens. */
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, UUID> {

  /** Finds all active device tokens for a given user. */
  List<UserDeviceToken> findByUserId(UUID userId);

  /** Finds a device token record by FCM token value. */
  Optional<UserDeviceToken> findByFcmToken(String fcmToken);

  /** Deletes a device token by FCM token value. */
  void deleteByFcmToken(String fcmToken);

  /** Deletes a specific device token for a user. */
  void deleteByUserIdAndFcmToken(UUID userId, String fcmToken);
}

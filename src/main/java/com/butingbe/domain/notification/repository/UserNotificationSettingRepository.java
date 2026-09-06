package com.butingbe.domain.notification.repository;

import com.butingbe.domain.notification.entity.UserNotificationSetting;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for user notification preference settings. */
public interface UserNotificationSettingRepository
    extends JpaRepository<UserNotificationSetting, UUID> {}

package com.butingbe.domain.notification.dto.response;

import com.butingbe.domain.notification.entity.UserNotificationSetting;

/** Response DTO representing user notification preference toggles. */
public record NotificationSettingResDto(
    boolean pushEnabled, boolean zoneEventEnabled, boolean settlementEnabled) {

  public static NotificationSettingResDto from(UserNotificationSetting setting) {
    if (setting == null) return new NotificationSettingResDto(true, true, true);
    return new NotificationSettingResDto(
        setting.getPushEnabled(), setting.getZoneEventEnabled(), setting.getSettlementEnabled());
  }
}

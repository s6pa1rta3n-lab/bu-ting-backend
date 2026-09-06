package com.butingbe.domain.notification.dto.request;

import com.butingbe.domain.notification.entity.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request DTO for registering an FCM device token. */
public record DeviceTokenRegisterReqDto(
    @NotBlank String fcmToken, @NotNull DeviceType deviceType) {}

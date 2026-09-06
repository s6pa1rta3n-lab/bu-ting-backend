package com.butingbe.domain.notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.notification.dto.request.DeviceTokenRegisterReqDto;
import com.butingbe.domain.notification.dto.request.NotificationSettingReqDto;
import com.butingbe.domain.notification.dto.response.NotificationSettingResDto;
import com.butingbe.domain.notification.entity.DeviceType;
import com.butingbe.domain.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class UserPushControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

  @Mock private NotificationService notificationService;
  @InjectMocks private UserPushController controller;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authenticatedUserResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
  }

  @Test
  @DisplayName("registerDeviceToken registers token")
  void registerDeviceToken() throws Exception {
    DeviceTokenRegisterReqDto req =
        new DeviceTokenRegisterReqDto("fcm_token_123", DeviceType.ANDROID);

    mockMvc
        .perform(
            post("/users/me/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(notificationService).registerDeviceToken(USER_ID, "fcm_token_123", DeviceType.ANDROID);
  }

  @Test
  @DisplayName("unregisterDeviceToken deletes token")
  void unregisterDeviceToken() throws Exception {
    mockMvc
        .perform(delete("/users/me/device-token").param("fcmToken", "fcm_token_123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(notificationService).unregisterDeviceToken(USER_ID, "fcm_token_123");
  }

  @Test
  @DisplayName("getNotificationSettings retrieves settings")
  void getNotificationSettings() throws Exception {
    when(notificationService.getNotificationSettings(USER_ID))
        .thenReturn(new NotificationSettingResDto(true, true, true));

    mockMvc
        .perform(get("/users/me/notification-settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.pushEnabled").value(true));
  }

  @Test
  @DisplayName("updateNotificationSettings updates settings")
  void updateNotificationSettings() throws Exception {
    NotificationSettingReqDto req = new NotificationSettingReqDto(false, false, true);
    when(notificationService.updateNotificationSettings(USER_ID, false, false, true))
        .thenReturn(new NotificationSettingResDto(false, false, true));

    mockMvc
        .perform(
            patch("/users/me/notification-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.pushEnabled").value(false));
  }

  @Test
  @DisplayName("getZoneSubscriptions returns user subscriptions")
  void getZoneSubscriptions() throws Exception {
    when(notificationService.getUserSubscriptions(USER_ID)).thenReturn(List.of());

    mockMvc
        .perform(get("/users/me/zone-subscriptions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @DisplayName("subscribeZone adds subscription")
  void subscribeZone() throws Exception {
    mockMvc
        .perform(post("/users/me/zone-subscriptions/{zoneId}", "GWANGAN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(notificationService).subscribeZone(USER_ID, "GWANGAN");
  }

  @Test
  @DisplayName("unsubscribeZone removes subscription")
  void unsubscribeZone() throws Exception {
    mockMvc
        .perform(delete("/users/me/zone-subscriptions/{zoneId}", "GWANGAN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(notificationService).unsubscribeZone(USER_ID, "GWANGAN");
  }

  private HandlerMethodArgumentResolver authenticatedUserResolver() {
    return new HandlerMethodArgumentResolver() {
      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
      }

      @Override
      public Object resolveArgument(
          MethodParameter parameter,
          ModelAndViewContainer mavContainer,
          NativeWebRequest webRequest,
          WebDataBinderFactory binderFactory) {
        return new AuthenticatedUser(USER_ID, "u@example.com", "u", List.of());
      }
    };
  }
}

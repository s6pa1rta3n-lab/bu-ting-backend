package com.butingbe.domain.notification.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.notification.dto.request.AdminPushSendReqDto;
import com.butingbe.domain.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminPushControllerTest {

  private static final UUID TARGET_USER_ID =
      UUID.fromString("11111111-0000-0000-0000-000000000001");

  @Mock private NotificationService notificationService;
  @InjectMocks private AdminPushController controller;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
  }

  @Test
  @DisplayName("sendPush sends to target user when userId present")
  void sendPush_user() throws Exception {
    AdminPushSendReqDto req = new AdminPushSendReqDto(null, TARGET_USER_ID, null, "푸시 제목", "푸시 본문");

    mockMvc
        .perform(
            post("/admin/notifications/push/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(notificationService).sendToUser(TARGET_USER_ID, "푸시 제목", "푸시 본문", "ADMIN");
  }

  @Test
  @DisplayName("sendPush sends to zone subscribers when zoneId present")
  void sendPush_zone() throws Exception {
    AdminPushSendReqDto req = new AdminPushSendReqDto(null, null, "GWANGAN", "푸시 제목", "푸시 본문");

    mockMvc
        .perform(
            post("/admin/notifications/push/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(notificationService).sendToZoneSubscribers("GWANGAN", "푸시 제목", "푸시 본문");
  }

  @Test
  @DisplayName("sendPush sends to topic when topic present")
  void sendPush_topic() throws Exception {
    AdminPushSendReqDto req = new AdminPushSendReqDto("all_users", null, null, "푸시 제목", "푸시 본문");

    mockMvc
        .perform(
            post("/admin/notifications/push/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(notificationService).sendTopic("all_users", "푸시 제목", "푸시 본문");
  }
}

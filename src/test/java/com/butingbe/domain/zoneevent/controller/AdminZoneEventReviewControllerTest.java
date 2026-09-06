package com.butingbe.domain.zoneevent.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.auth.security.OperatorAuthorization;
import com.butingbe.domain.zoneevent.dto.request.ReviewRejectReqDto;
import com.butingbe.domain.zoneevent.dto.response.ReviewQueuePageResDto;
import com.butingbe.domain.zoneevent.service.AdminZoneEventReviewService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class AdminZoneEventReviewControllerTest {

  private static final UUID OPERATOR_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID PARTICIPATION_ID =
      UUID.fromString("22222222-0000-0000-0000-000000000002");

  @Mock private AdminZoneEventReviewService reviewService;
  @Mock private OperatorAuthorization operatorAuthorization;
  @InjectMocks private AdminZoneEventReviewController controller;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authenticatedAdminResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
  }

  @Test
  @DisplayName("getReviewQueue returns list of queue items")
  void getReviewQueue() throws Exception {
    when(reviewService.getReviewQueue(0, 20))
        .thenReturn(new ReviewQueuePageResDto(List.of(), 0, false));

    mockMvc
        .perform(get("/admin/zone-events/reviews/queue"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @DisplayName("approveParticipation triggers approve action")
  void approveParticipation() throws Exception {
    mockMvc
        .perform(post("/admin/zone-events/reviews/{id}/approve", PARTICIPATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(reviewService).approve(OPERATOR_ID, PARTICIPATION_ID);
  }

  @Test
  @DisplayName("rejectParticipation triggers reject action")
  void rejectParticipation() throws Exception {
    ReviewRejectReqDto req = new ReviewRejectReqDto("인증 사진 불일치");

    mockMvc
        .perform(
            post("/admin/zone-events/reviews/{id}/reject", PARTICIPATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(reviewService).reject(OPERATOR_ID, PARTICIPATION_ID, "인증 사진 불일치");
  }

  @Test
  @DisplayName("revokeParticipation triggers revoke action")
  void revokeParticipation() throws Exception {
    mockMvc
        .perform(
            post("/admin/zone-events/reviews/{id}/revoke", PARTICIPATION_ID)
                .param("reason", "부정 인증 사후 발견"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(reviewService).revoke(OPERATOR_ID, PARTICIPATION_ID, "부정 인증 사후 발견");
  }

  @Test
  @DisplayName("unhideParticipation triggers unhide action")
  void unhideParticipation() throws Exception {
    mockMvc
        .perform(post("/admin/zone-events/reviews/{id}/unhide", PARTICIPATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(reviewService).unhide(OPERATOR_ID, PARTICIPATION_ID);
  }

  private HandlerMethodArgumentResolver authenticatedAdminResolver() {
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
        return new AuthenticatedUser(
            OPERATOR_ID,
            "admin@example.com",
            "admin",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
      }
    };
  }
}

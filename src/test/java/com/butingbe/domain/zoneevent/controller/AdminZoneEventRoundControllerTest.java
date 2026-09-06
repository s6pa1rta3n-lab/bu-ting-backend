package com.butingbe.domain.zoneevent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.auth.security.OperatorAuthorization;
import com.butingbe.domain.zoneevent.dto.response.AutoAssignRecommendationResDto;
import com.butingbe.domain.zoneevent.dto.response.RoundResDto;
import com.butingbe.domain.zoneevent.dto.response.SlotResDto;
import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.RoundType;
import com.butingbe.domain.zoneevent.entity.SlotKind;
import com.butingbe.domain.zoneevent.service.AdminZoneEventRoundService;
import java.time.OffsetDateTime;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class AdminZoneEventRoundControllerTest {

  private static final UUID OPERATOR_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID ROUND_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");
  private static final UUID SLOT_ID = UUID.fromString("33333333-0000-0000-0000-000000000003");
  private static final UUID EVENT_ID = UUID.fromString("44444444-0000-0000-0000-000000000004");

  @Mock private AdminZoneEventRoundService roundService;
  @Mock private OperatorAuthorization operatorAuthorization;
  @InjectMocks private AdminZoneEventRoundController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(
                authenticatedAdminResolver(),
                new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
  }

  @Test
  @DisplayName("createRound creates a new round")
  void createRound() throws Exception {
    OffsetDateTime start = OffsetDateTime.now();
    OffsetDateTime end = start.plusDays(7);
    RoundResDto res =
        new RoundResDto(
            ROUND_ID,
            RoundType.REGULAR,
            start,
            end,
            "Asia/Seoul",
            RoundStatus.SCHEDULED,
            null,
            List.of(),
            List.of());

    when(roundService.createRound(any(), eq(OPERATOR_ID))).thenReturn(res);

    String json =
        """
        {
          "roundType": "REGULAR",
          "startsAt": "2026-09-01T00:00:00Z",
          "endsAt": "2026-09-08T00:00:00Z",
          "timezone": "Asia/Seoul",
          "slots": [],
          "backupTargets": []
        }
        """;

    mockMvc
        .perform(
            post("/admin/zone-events/rounds").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.roundId").value(ROUND_ID.toString()));
  }

  @Test
  @DisplayName("getRound retrieves round details")
  void getRound() throws Exception {
    RoundResDto res =
        new RoundResDto(
            ROUND_ID,
            RoundType.REGULAR,
            OffsetDateTime.now(),
            OffsetDateTime.now().plusDays(7),
            "Asia/Seoul",
            RoundStatus.OPEN,
            null,
            List.of(),
            List.of());

    when(roundService.getRound(ROUND_ID)).thenReturn(res);

    mockMvc
        .perform(get("/admin/zone-events/rounds/{id}", ROUND_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.roundId").value(ROUND_ID.toString()));
  }

  @Test
  @DisplayName("listRounds returns paginated rounds")
  void listRounds() throws Exception {
    when(roundService.listRounds(any()))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    mockMvc
        .perform(get("/admin/zone-events/rounds"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @DisplayName("replaceSlot updates round slot event")
  void replaceSlot() throws Exception {
    SlotResDto res =
        new SlotResDto(SLOT_ID, "GWANGAN", SlotKind.AUTH, EVENT_ID, null, "이벤트 제목", null);

    when(roundService.replaceSlot(ROUND_ID, SLOT_ID, EVENT_ID, OPERATOR_ID)).thenReturn(res);

    String json = String.format("{\"newEventId\": \"%s\"}", EVENT_ID);

    mockMvc
        .perform(
            patch("/admin/zone-events/rounds/{roundId}/slots/{slotId}/replace", ROUND_ID, SLOT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.eventId").value(EVENT_ID.toString()));
  }

  @Test
  @DisplayName("replaceRainTarget replaces auth target with backup")
  void replaceRainTarget() throws Exception {
    String json =
        String.format(
            """
        {
          "targetEventId": "%s",
          "backupTargetId": "%s"
        }
        """,
            EVENT_ID, UUID.randomUUID());

    mockMvc
        .perform(
            post("/admin/zone-events/rounds/{roundId}/rain-target/replace", ROUND_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(roundService).replaceRainTarget(eq(ROUND_ID), any(), eq(OPERATOR_ID));
  }

  @Test
  @DisplayName("recommendAutoAssign returns recommendations")
  void recommendAutoAssign() throws Exception {
    when(roundService.recommendAutoAssign())
        .thenReturn(new AutoAssignRecommendationResDto(List.of()));

    mockMvc
        .perform(get("/admin/zone-events/rounds/recommendations/auto-assign"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
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

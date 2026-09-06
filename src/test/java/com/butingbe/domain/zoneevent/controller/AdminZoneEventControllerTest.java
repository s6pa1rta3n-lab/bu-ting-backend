package com.butingbe.domain.zoneevent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.response.AdminZoneEventPageResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventDetailResDto;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind;
import com.butingbe.domain.zoneevent.entity.ZoneEventType;
import com.butingbe.domain.zoneevent.service.AdminZoneEventService;
import com.butingbe.global.error.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

@ExtendWith(MockitoExtension.class)
class AdminZoneEventControllerTest {

  private static final UUID ADMIN_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();

  @Mock private AdminZoneEventService adminZoneEventService;
  @InjectMocks private AdminZoneEventController controller;

  private MockMvc mockMvc;
  private AuthenticatedUser adminUser;

  @BeforeEach
  void setUp() {
    adminUser =
        new AuthenticatedUser(
            ADMIN_ID,
            "admin@example.com",
            "admin",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    StaticMessageSource messageSource = new StaticMessageSource();
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authenticatedUserResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .setControllerAdvice(
                new GlobalExceptionHandler(messageSource, new FixedLocaleResolver(Locale.KOREAN)))
            .build();
  }

  @Test
  @DisplayName("관리자 이벤트 목록 조회는 200 OK와 페이징 데이터를 반환한다")
  void getEventsReturns200() throws Exception {
    when(adminZoneEventService.getEvents(any(), any(), any(), any(), any(), eq(0), eq(20)))
        .thenReturn(new AdminZoneEventPageResDto(List.of(), 0, 20, 0L, 0));

    mockMvc
        .perform(get("/admin/zone-events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @DisplayName("관리자 이벤트 상세 조회는 200 OK와 이벤트 상세를 반환한다")
  void getEventDetailReturns200() throws Exception {
    when(adminZoneEventService.getEventDetail(any(), eq(EVENT_ID)))
        .thenReturn(sampleDetail("SCHEDULED"));

    mockMvc
        .perform(get("/admin/zone-events/{eventId}", EVENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
  }

  @Test
  @DisplayName("이벤트 생성 호출 시 201 Created와 생성된 이벤트를 반환한다")
  void createEventReturns201() throws Exception {
    when(adminZoneEventService.createEvent(any(), any())).thenReturn(sampleDetail("SCHEDULED"));

    String requestJson =
        """
        {
          "zoneId": "SUYEONG_NAMGU",
          "typeCode": "PLACE_AUTH",
          "title": "광안리 행사",
          "description": "설명",
          "startsAt": "2026-09-01T00:00:00Z",
          "durationMinutes": 60,
          "target": {
            "targetKind": "PLACE",
            "placeId": "p1",
            "placeName": "광안대교",
            "latitude": 35.153,
            "longitude": 129.118,
            "radiusM": 100
          },
          "baseReward": {
            "amount": 50,
            "catalogCode": "SPOT_GWANGAN_BRIDGE"
          },
          "successLimitPerUser": 1
        }
        """;

    mockMvc
        .perform(post("/admin/zone-events").contentType("application/json").content(requestJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
  }

  @Test
  @DisplayName("이벤트 활성화 호출 시 200 OK와 ACTIVE 상태를 반환한다")
  void activateEventReturns200() throws Exception {
    when(adminZoneEventService.activateEvent(any(), eq(EVENT_ID)))
        .thenReturn(sampleDetail("ACTIVE"));

    mockMvc
        .perform(post("/admin/zone-events/{eventId}/activate", EVENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
  }

  @Test
  @DisplayName("이벤트 종료 호출 시 200 OK와 CLOSED 상태를 반환한다")
  void closeEventReturns200() throws Exception {
    when(adminZoneEventService.closeEvent(any(), eq(EVENT_ID))).thenReturn(sampleDetail("CLOSED"));

    mockMvc
        .perform(post("/admin/zone-events/{eventId}/close", EVENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CLOSED"));
  }

  @Test
  @DisplayName("이벤트 취소 호출 시 200 OK와 CANCELLED 상태를 반환한다")
  void cancelEventReturns200() throws Exception {
    when(adminZoneEventService.cancelEvent(any(), eq(EVENT_ID)))
        .thenReturn(sampleDetail("CANCELLED"));

    mockMvc
        .perform(post("/admin/zone-events/{eventId}/cancel", EVENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("이벤트 수정 시 200 OK를 반환한다")
  void updateEventReturns200() throws Exception {
    when(adminZoneEventService.updateEvent(any(), eq(EVENT_ID), any()))
        .thenReturn(sampleDetail("SCHEDULED"));

    mockMvc
        .perform(
            patch("/admin/zone-events/{eventId}", EVENT_ID)
                .contentType("application/json")
                .content("{\"title\":\"새 제목\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("새 제목"));
  }

  private ZoneEventDetailResDto sampleDetail(String status) {
    ZoneEventType type =
        ZoneEventType.builder().typeCode("PLACE_AUTH").name("장소 인증").requiresUpload(true).build();
    ZoneEvent event =
        ZoneEvent.builder()
            .zoneId("SUYEONG_NAMGU")
            .type(type)
            .title("새 제목")
            .description("설명")
            .startsAt(OffsetDateTime.now())
            .durationMinutes(60)
            .status(ZoneEventStatus.valueOf(status))
            .successLimitPerUser(1)
            .build();
    ReflectionTestUtils.setField(event, "id", EVENT_ID);

    ZoneEventAuthTarget target =
        ZoneEventAuthTarget.builder()
            .event(event)
            .targetKind(ZoneEventTargetKind.PLACE)
            .placeName("광안대교")
            .latitude(35.153)
            .longitude(129.118)
            .radiusM(100)
            .build();
    ReflectionTestUtils.setField(target, "id", UUID.randomUUID());

    return ZoneEventDetailResDto.of(event, target, null, 3600L, 0L, null);
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
        return adminUser;
      }
    };
  }
}

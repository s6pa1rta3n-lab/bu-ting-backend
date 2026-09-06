package com.butingbe.domain.zoneevent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.response.ParticipationResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventParticipationPageResDto;
import com.butingbe.domain.zoneevent.service.ZoneEventParticipationService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

@ExtendWith(MockitoExtension.class)
class UserZoneEventParticipationControllerTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID PARTICIPATION_ID = UUID.randomUUID();

  @Mock private ZoneEventParticipationService participationService;
  @InjectMocks private UserZoneEventParticipationController controller;

  private MockMvc mockMvc;
  private AuthenticatedUser currentUser;

  @BeforeEach
  void setUp() {
    currentUser = new AuthenticatedUser(USER_ID, "user@example.com", "user", List.of());
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
  @DisplayName("내 전체 이벤트 참여 이력 조회 시 200과 페이징 데이터를 반환한다")
  void getMyParticipationsSuccess() throws Exception {
    ParticipationResDto item =
        new ParticipationResDto(
            PARTICIPATION_ID.toString(),
            EVENT_ID.toString(),
            "SUYEONG_NAMGU",
            "PLACE_AUTH",
            "SUCCESS",
            true,
            null,
            null,
            "인증 완료",
            0,
            "PUBLIC",
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            List.of());

    when(participationService.getMyParticipations(
            any(), any(), eq(20), any(), any(), any(), any(), any()))
        .thenReturn(new ZoneEventParticipationPageResDto(List.of(item), null, false));

    mockMvc
        .perform(get("/users/me/zone-event-participations").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.items[0].participationId").value(PARTICIPATION_ID.toString()))
        .andExpect(jsonPath("$.data.hasNext").value(false));
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
        return currentUser;
      }
    };
  }
}

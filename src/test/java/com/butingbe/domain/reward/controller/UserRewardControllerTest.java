package com.butingbe.domain.reward.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.chat.entity.ChatZone;
import com.butingbe.domain.reward.dto.response.PointLedgerEntryResDto;
import com.butingbe.domain.reward.dto.response.PointLedgerPageResDto;
import com.butingbe.domain.reward.dto.response.UserBadgeItemResDto;
import com.butingbe.domain.reward.dto.response.UserRewardsResDto;
import com.butingbe.domain.reward.dto.response.ZoneBadgeGroupResDto;
import com.butingbe.domain.reward.service.UserRewardService;
import com.butingbe.global.error.GlobalExceptionHandler;
import com.butingbe.global.error.exception.UnauthenticatedException;
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
class UserRewardControllerTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private UserRewardService userRewardService;
  @InjectMocks private UserRewardController controller;

  private MockMvc mockMvc;
  private AuthenticatedUser currentUser;

  @BeforeEach
  void setUp() {
    currentUser = new AuthenticatedUser(USER_ID, "user@example.com", "user", List.of());
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage("error.auth.unauthenticated", Locale.KOREAN, "인증이 필요합니다.");
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authenticatedUserResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .setControllerAdvice(
                new GlobalExceptionHandler(messageSource, new FixedLocaleResolver(Locale.KOREAN)))
            .build();
  }

  @Test
  @DisplayName("내 보상 현황 조회 시 200과 보상 데이터를 반환한다")
  void getMyRewardsSuccess() throws Exception {
    List<ZoneBadgeGroupResDto> badges =
        List.of(
            new ZoneBadgeGroupResDto(
                ChatZone.SUYEONG_NAMGU.name(),
                ChatZone.SUYEONG_NAMGU.getZoneName(),
                List.of(
                    new UserBadgeItemResDto(
                        UUID.randomUUID().toString(),
                        "SPOT_GWANGAN",
                        "광안 배지",
                        null,
                        OffsetDateTime.now()))));

    when(userRewardService.getMyRewards(any()))
        .thenReturn(new UserRewardsResDto(200L, badges, List.of()));

    mockMvc
        .perform(get("/users/me/rewards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.pointBalance").value(200))
        .andExpect(jsonPath("$.data.badges[0].zoneId").value("SUYEONG_NAMGU"));
  }

  @Test
  @DisplayName("포인트 원장 조회 시 200과 페이징 목록을 반환한다")
  void getPointLedgerSuccess() throws Exception {
    List<PointLedgerEntryResDto> items =
        List.of(
            new PointLedgerEntryResDto(
                UUID.randomUUID().toString(), 50, "BASE", null, OffsetDateTime.now()));

    when(userRewardService.getPointLedger(any(), eq(null), eq(20)))
        .thenReturn(new PointLedgerPageResDto(items, null, false));

    mockMvc
        .perform(get("/users/me/point-ledger"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.items[0].amount").value(50))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  @DisplayName("미인증 유저 조회 시 401 Unauthorized를 반환한다")
  void unauthenticatedReturns401() throws Exception {
    when(userRewardService.getMyRewards(any())).thenThrow(new UnauthenticatedException());

    mockMvc.perform(get("/users/me/rewards")).andExpect(status().isUnauthorized());
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

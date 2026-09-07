package com.butingbe.domain.reward.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.auth.security.OperatorAuthorization;
import com.butingbe.domain.reward.dto.response.RewardPayoutDetailResDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutHistoryResDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutPageResDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutSnapshotDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutSummaryResDto;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardPayoutAction;
import com.butingbe.domain.reward.entity.RewardPayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import com.butingbe.domain.reward.exception.RewardPayoutConflictException;
import com.butingbe.domain.reward.service.AdminRewardPayoutService;
import com.butingbe.global.error.GlobalExceptionHandler;
import com.butingbe.global.error.exception.ForbiddenException;
import java.time.LocalDateTime;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

@ExtendWith(MockitoExtension.class)
class AdminRewardPayoutControllerTest {

  private static final UUID PAYOUT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
  private static final UUID USER_ID = UUID.fromString("22222222-0000-0000-0000-000000000001");
  private static final UUID ROUND_ID = UUID.fromString("33333333-0000-0000-0000-000000000001");
  private static final UUID EVENT_ID = UUID.fromString("44444444-0000-0000-0000-000000000001");
  private static final UUID PARTICIPATION_ID =
      UUID.fromString("55555555-0000-0000-0000-000000000001");

  @Mock private AdminRewardPayoutService adminRewardPayoutService;
  @Mock private OperatorAuthorization operatorAuthorization;
  @InjectMocks private AdminRewardPayoutController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage("error.operator.forbidden", Locale.KOREAN, "운영 권한이 없습니다.");
    messageSource.addMessage(
        "error.reward_payout.conflict", Locale.KOREAN, "보상 지급 처리 중 충돌이 발생했습니다.");
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authenticatedUserResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .setValidator(validator)
            .setControllerAdvice(
                new GlobalExceptionHandler(messageSource, new FixedLocaleResolver(Locale.KOREAN)))
            .build();
  }

  @Test
  @DisplayName("목록 조회는 200과 페이징 결과를 반환한다")
  void list() throws Exception {
    when(adminRewardPayoutService.list(
            any(), any(), any(), any(), any(), any(), any(), eq(0), eq(20)))
        .thenReturn(
            new RewardPayoutPageResDto(
                List.of(createSummary(PAYOUT_ID, RewardPayoutStatus.PENDING_CONFIRM)),
                0,
                20,
                1L,
                1,
                false));

    mockMvc
        .perform(get("/admin/reward-payouts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].payoutId").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("상세 조회는 200과 세부 정보 및 이력을 반환한다")
  void detail() throws Exception {
    when(adminRewardPayoutService.detail(PAYOUT_ID)).thenReturn(createDetail(PAYOUT_ID));

    mockMvc
        .perform(get("/admin/reward-payouts/{payoutId}", PAYOUT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.payoutId").value(PAYOUT_ID.toString()))
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
  }

  @Test
  @DisplayName("항목 수정은 200을 반환한다")
  void patchPayout() throws Exception {
    when(adminRewardPayoutService.patch(any(), eq(PAYOUT_ID), any()))
        .thenReturn(createDetail(PAYOUT_ID));

    mockMvc
        .perform(
            patch("/admin/reward-payouts/{payoutId}", PAYOUT_ID)
                .contentType("application/json")
                .content("{\"points\":100,\"note\":\"수정\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.payoutId").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("일괄 확정 성공 시 200을 반환한다")
  void bulkConfirmSuccess() throws Exception {
    when(adminRewardPayoutService.bulkConfirm(any(), any())).thenReturn(List.of(PAYOUT_ID));

    mockMvc
        .perform(
            post("/admin/reward-payouts/bulk-confirm")
                .contentType("application/json")
                .content("{\"payoutIds\":[\"" + PAYOUT_ID + "\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0]").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("일괄 확정 시 충돌이나 조건 불만족이면 409와 문제 ID 목록을 반환한다")
  void bulkConfirmConflict() throws Exception {
    when(adminRewardPayoutService.bulkConfirm(any(), any()))
        .thenThrow(
            new RewardPayoutConflictException("error.reward_payout.conflict", List.of(PAYOUT_ID)));

    mockMvc
        .perform(
            post("/admin/reward-payouts/bulk-confirm")
                .contentType("application/json")
                .content("{\"payoutIds\":[\"" + PAYOUT_ID + "\"]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.data.problematicPayoutIds[0]").value(PAYOUT_ID.toString()))
        .andExpect(jsonPath("$.data.payoutIds[0]").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("일괄 일정 변경은 200을 반환한다")
  void bulkSchedule() throws Exception {
    when(adminRewardPayoutService.bulkSchedule(any(), any())).thenReturn(List.of(PAYOUT_ID));

    mockMvc
        .perform(
            post("/admin/reward-payouts/bulk-schedule")
                .contentType("application/json")
                .content(
                    "{\"payoutIds\":[\""
                        + PAYOUT_ID
                        + "\"],\"scheduledAt\":\"2026-09-10T12:00:00+09:00\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0]").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("메일 발송 기록은 200을 반환한다")
  void markMailSent() throws Exception {
    when(adminRewardPayoutService.markMailSent(any(), any())).thenReturn(List.of(PAYOUT_ID));

    mockMvc
        .perform(
            post("/admin/reward-payouts/mark-mail-sent")
                .contentType("application/json")
                .content("{\"payoutIds\":[\"" + PAYOUT_ID + "\"],\"note\":\"안내 발송\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0]").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("배송 정보 수집 기록은 200을 반환한다")
  void markInfoCollected() throws Exception {
    when(adminRewardPayoutService.markInfoCollected(any(), any())).thenReturn(List.of(PAYOUT_ID));

    mockMvc
        .perform(
            post("/admin/reward-payouts/mark-info-collected")
                .contentType("application/json")
                .content("{\"payoutIds\":[\"" + PAYOUT_ID + "\"],\"note\":\"정보 수집\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0]").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("발송 완료 기록은 200을 반환한다")
  void markSent() throws Exception {
    when(adminRewardPayoutService.markSent(any(), any())).thenReturn(List.of(PAYOUT_ID));

    mockMvc
        .perform(
            post("/admin/reward-payouts/mark-sent")
                .contentType("application/json")
                .content("{\"payoutIds\":[\"" + PAYOUT_ID + "\"],\"reference\":\"TRACK-123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0]").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("재시도는 200을 반환한다")
  void retry() throws Exception {
    when(adminRewardPayoutService.retry(any(), eq(PAYOUT_ID))).thenReturn(createDetail(PAYOUT_ID));

    mockMvc
        .perform(post("/admin/reward-payouts/{payoutId}/retry", PAYOUT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.payoutId").value(PAYOUT_ID.toString()));
  }

  @Test
  @DisplayName("보류 및 보류 해제는 200을 반환한다")
  void holdAndRelease() throws Exception {
    when(adminRewardPayoutService.hold(any(), eq(PAYOUT_ID), any()))
        .thenReturn(createDetail(PAYOUT_ID));
    when(adminRewardPayoutService.releaseHold(any(), eq(PAYOUT_ID), any()))
        .thenReturn(createDetail(PAYOUT_ID));

    mockMvc
        .perform(
            post("/admin/reward-payouts/{payoutId}/hold", PAYOUT_ID)
                .contentType("application/json")
                .content("{\"note\":\"신고 검토\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/admin/reward-payouts/{payoutId}/release-hold", PAYOUT_ID)
                .contentType("application/json")
                .content("{\"note\":\"신고 기각\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("운영자 권한이 없으면 403을 반환한다")
  void forbidden() throws Exception {
    doThrow(new ForbiddenException("error.operator.forbidden"))
        .when(operatorAuthorization)
        .requireOperator(any());

    mockMvc.perform(get("/admin/reward-payouts")).andExpect(status().isForbidden());
  }

  private RewardPayoutSummaryResDto createSummary(UUID id, RewardPayoutStatus status) {
    return new RewardPayoutSummaryResDto(
        id,
        ROUND_ID,
        EVENT_ID,
        PARTICIPATION_ID,
        USER_ID,
        GrantReason.BASE.name(),
        null,
        null,
        status,
        RewardPayoutHoldStatus.NONE,
        null,
        50,
        "BADGE_SPOT",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        null,
        null,
        0L,
        LocalDateTime.now(),
        LocalDateTime.now());
  }

  private RewardPayoutDetailResDto createDetail(UUID id) {
    return new RewardPayoutDetailResDto(
        id,
        ROUND_ID,
        EVENT_ID,
        PARTICIPATION_ID,
        USER_ID,
        GrantReason.BASE.name(),
        null,
        null,
        RewardPayoutStatus.CONFIRMED,
        RewardPayoutHoldStatus.NONE,
        new RewardPayoutSnapshotDto(null, null, null, null, 50, "BADGE_SPOT", null),
        null,
        OffsetDateTime.now(),
        USER_ID,
        null,
        null,
        null,
        null,
        null,
        0,
        null,
        null,
        1L,
        LocalDateTime.now(),
        LocalDateTime.now(),
        List.of(
            new RewardPayoutHistoryResDto(
                UUID.randomUUID(),
                RewardPayoutStatus.PENDING_CONFIRM,
                RewardPayoutStatus.CONFIRMED,
                RewardPayoutAction.CONFIRM,
                USER_ID,
                "Confirmed",
                OffsetDateTime.now())));
  }

  private HandlerMethodArgumentResolver authenticatedUserResolver() {
    return new HandlerMethodArgumentResolver() {
      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
            && AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
      }

      @Override
      public Object resolveArgument(
          MethodParameter parameter,
          ModelAndViewContainer mavContainer,
          NativeWebRequest webRequest,
          WebDataBinderFactory binderFactory) {
        return new AuthenticatedUser(USER_ID, "admin@test.com", "admin", List.of());
      }
    };
  }
}

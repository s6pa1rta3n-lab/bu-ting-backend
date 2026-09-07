package com.butingbe.domain.reward.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.dto.response.AdminPayoutItemResDto;
import com.butingbe.domain.reward.entity.PayoutHoldStatus;
import com.butingbe.domain.reward.service.RewardPayoutHoldService;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.global.error.GlobalExceptionHandler;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
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

  private static final UUID PAYOUT_ID = UUID.fromString("55555555-0000-0000-0000-000000000001");
  private static final UUID PARTICIPATION_ID =
      UUID.fromString("22222222-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("33333333-0000-0000-0000-000000000001");

  @Mock private RewardPayoutHoldService rewardPayoutHoldService;
  @InjectMocks private AdminRewardPayoutController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage("error.operator.forbidden", Locale.KOREAN, "운영 권한이 없습니다.");
    messageSource.addMessage("error.reward.payout.not_found", Locale.KOREAN, "보상 지급 건을 찾을 수 없습니다.");
    messageSource.addMessage(
        "error.reward.payout.unresolved_reports", Locale.KOREAN, "미해결된 신고가 있어 보류를 해제할 수 없습니다.");
    messageSource.addMessage("error.revision.conflict", Locale.KOREAN, "데이터가 이미 변경되었습니다.");

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
  @DisplayName("보류 해제 요청 성공 시 200과 갱신된 지급 정보를 반환한다")
  void releaseHoldSuccess() throws Exception {
    AdminPayoutItemResDto resDto =
        new AdminPayoutItemResDto(
            PAYOUT_ID,
            "BASE",
            PARTICIPATION_ID,
            null,
            null,
            null,
            new RewardSnapshot(50, null, null, null),
            "READY",
            PayoutHoldStatus.NONE,
            1L,
            LocalDateTime.now(),
            LocalDateTime.now());

    when(rewardPayoutHoldService.releaseHold(any(), eq(PAYOUT_ID), any())).thenReturn(resDto);

    mockMvc
        .perform(
            post("/admin/reward-payouts/{payoutId}/release-hold", PAYOUT_ID)
                .contentType("application/json")
                .content("{\"note\":\"이상 없음\",\"expectedRevision\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.payoutId").value(PAYOUT_ID.toString()))
        .andExpect(jsonPath("$.data.holdStatus").value("NONE"))
        .andExpect(jsonPath("$.data.status").value("READY"));
  }

  @Test
  @DisplayName("미해결 신고가 남아있을 때 보류 해제를 시도하면 409를 반환한다")
  void releaseHoldConflictUnresolvedReports() throws Exception {
    when(rewardPayoutHoldService.releaseHold(any(), eq(PAYOUT_ID), any()))
        .thenThrow(new ConflictException("error.reward.payout.unresolved_reports"));

    mockMvc
        .perform(
            post("/admin/reward-payouts/{payoutId}/release-hold", PAYOUT_ID)
                .contentType("application/json")
                .content("{\"note\":\"해제 시도\",\"expectedRevision\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("미해결된 신고가 있어 보류를 해제할 수 없습니다."));
  }

  @Test
  @DisplayName("리비전 충돌 시 409를 반환한다")
  void releaseHoldRevisionConflict() throws Exception {
    when(rewardPayoutHoldService.releaseHold(any(), eq(PAYOUT_ID), any()))
        .thenThrow(new ConflictException("error.revision.conflict"));

    mockMvc
        .perform(
            post("/admin/reward-payouts/{payoutId}/release-hold", PAYOUT_ID)
                .contentType("application/json")
                .content("{\"note\":\"해제 시도\",\"expectedRevision\":99}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("데이터가 이미 변경되었습니다."));
  }

  @Test
  @DisplayName("존재하지 않는 지급 건에 대해 보류 해제를 시도하면 404를 반환한다")
  void releaseHoldNotFound() throws Exception {
    when(rewardPayoutHoldService.releaseHold(any(), eq(PAYOUT_ID), any()))
        .thenThrow(new ResourceNotFoundException("error.reward.payout.not_found"));

    mockMvc
        .perform(
            post("/admin/reward-payouts/{payoutId}/release-hold", PAYOUT_ID)
                .contentType("application/json")
                .content("{\"note\":\"해제 시도\",\"expectedRevision\":0}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("보상 지급 건을 찾을 수 없습니다."));
  }

  @Test
  @DisplayName("운영자 권한이 없으면 403을 반환한다")
  void releaseHoldForbidden() throws Exception {
    when(rewardPayoutHoldService.releaseHold(any(), eq(PAYOUT_ID), any()))
        .thenThrow(new ForbiddenException("error.operator.forbidden"));

    mockMvc
        .perform(
            post("/admin/reward-payouts/{payoutId}/release-hold", PAYOUT_ID)
                .contentType("application/json")
                .content("{\"note\":\"해제 시도\",\"expectedRevision\":0}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("운영 권한이 없습니다."));
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
        return new AuthenticatedUser(USER_ID, "admin@example.com", "admin", List.of());
      }
    };
  }
}

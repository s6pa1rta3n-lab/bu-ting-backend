package com.butingbe.domain.reward.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.dto.response.RewardCatalogResDto;
import com.butingbe.domain.reward.dto.response.RewardGrantPageResDto;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.service.AdminRewardCatalogService;
import com.butingbe.global.error.GlobalExceptionHandler;
import com.butingbe.global.error.exception.ForbiddenException;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

@ExtendWith(MockitoExtension.class)
class AdminRewardCatalogControllerTest {

  private static final UUID ADMIN_ID = UUID.randomUUID();
  private static final UUID REWARD_ID = UUID.randomUUID();

  @Mock private AdminRewardCatalogService adminRewardCatalogService;
  @InjectMocks private AdminRewardCatalogController controller;

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
    messageSource.addMessage("error.operator.forbidden", Locale.KOREAN, "접근 권한이 없습니다.");

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authenticatedUserResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .setControllerAdvice(
                new GlobalExceptionHandler(messageSource, new FixedLocaleResolver(Locale.KOREAN)))
            .build();
  }

  @Test
  @DisplayName("카탈로그 목록 조회는 200과 목록을 반환한다")
  void getCatalogReturns200() throws Exception {
    when(adminRewardCatalogService.getCatalog(any(), eq(RewardType.POINT), eq(true)))
        .thenReturn(
            List.of(
                new RewardCatalogResDto(
                    REWARD_ID.toString(),
                    RewardType.POINT,
                    "PT_100",
                    "100포인트",
                    100,
                    null,
                    null,
                    1000,
                    10,
                    null,
                    true,
                    LocalDateTime.now(),
                    LocalDateTime.now())));

    mockMvc
        .perform(get("/admin/reward-catalog").param("rewardType", "POINT").param("active", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].code").value("PT_100"));
  }

  @Test
  @DisplayName("신규 카탈로그 생성은 201 Created를 반환한다")
  void createCatalogReturns201() throws Exception {
    when(adminRewardCatalogService.createCatalog(any(), any()))
        .thenReturn(
            new RewardCatalogResDto(
                REWARD_ID.toString(),
                RewardType.POINT,
                "PT_100",
                "100포인트",
                100,
                null,
                null,
                1000,
                10,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()));

    mockMvc
        .perform(
            post("/admin/reward-catalog")
                .contentType("application/json")
                .content(
                    "{\"rewardType\":\"POINT\",\"code\":\"PT_100\",\"name\":\"100포인트\",\"pointAmount\":100}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.code").value("PT_100"));
  }

  @Test
  @DisplayName("카탈로그 수정은 200 OK를 반환한다")
  void updateCatalogReturns200() throws Exception {
    when(adminRewardCatalogService.updateCatalog(any(), eq(REWARD_ID), any()))
        .thenReturn(
            new RewardCatalogResDto(
                REWARD_ID.toString(),
                RewardType.POINT,
                "PT_100",
                "수정된 포인트",
                100,
                null,
                null,
                500,
                5,
                null,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()));

    mockMvc
        .perform(
            patch("/admin/reward-catalog/{rewardId}", REWARD_ID)
                .contentType("application/json")
                .content("{\"name\":\"수정된 포인트\",\"stock\":500,\"active\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("수정된 포인트"))
        .andExpect(jsonPath("$.data.stock").value(500));
  }

  @Test
  @DisplayName("발급 내역 조회는 200 OK와 페이징 데이터를 반환한다")
  void getGrantsReturns200() throws Exception {
    when(adminRewardCatalogService.getGrants(any(), eq(REWARD_ID), any(), eq(20)))
        .thenReturn(new RewardGrantPageResDto(List.of(), null, false));

    mockMvc
        .perform(get("/admin/reward-catalog/{rewardId}/grants", REWARD_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  @DisplayName("비인가 사용자가 접근하면 403 Forbidden을 반환한다")
  void forbiddenReturns403() throws Exception {
    when(adminRewardCatalogService.getCatalog(any(), any(), any()))
        .thenThrow(new ForbiddenException("error.operator.forbidden"));

    mockMvc.perform(get("/admin/reward-catalog")).andExpect(status().isForbidden());
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

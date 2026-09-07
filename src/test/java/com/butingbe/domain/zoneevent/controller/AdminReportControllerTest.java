package com.butingbe.domain.zoneevent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.response.AdminReportDetailResDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportPageResDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportSummaryResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ReportReasonCode;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.service.AdminReportService;
import com.butingbe.global.error.GlobalExceptionHandler;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
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
class AdminReportControllerTest {

  private static final UUID REPORT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID PARTICIPATION_ID =
      UUID.fromString("22222222-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("33333333-0000-0000-0000-000000000001");
  private static final UUID EVENT_ID = UUID.fromString("44444444-0000-0000-0000-000000000001");

  @Mock private AdminReportService adminReportService;
  @InjectMocks private AdminReportController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage("error.operator.forbidden", Locale.KOREAN, "운영 권한이 없습니다.");
    messageSource.addMessage("error.zone_event.report.not_found", Locale.KOREAN, "신고를 찾을 수 없습니다.");
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
  @DisplayName("신고 목록 조회는 200을 반환한다")
  void getReports() throws Exception {
    AdminReportSummaryResDto item =
        new AdminReportSummaryResDto(
            REPORT_ID,
            USER_ID,
            ReportReasonCode.NOT_ON_SITE,
            "현장 아님",
            ReportStatus.OPEN,
            OffsetDateTime.now(),
            null,
            null,
            null,
            0L,
            PARTICIPATION_ID,
            EVENT_ID,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null);
    AdminReportPageResDto pageRes = new AdminReportPageResDto(List.of(item), 0, 10, 1L, 1);

    when(adminReportService.getReports(any(), any(), any(), any(), any(), any()))
        .thenReturn(pageRes);

    mockMvc
        .perform(
            get("/admin/zone-event-reports")
                .param("status", "OPEN")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].reportId").value(REPORT_ID.toString()))
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  @DisplayName("신고 상세 조회는 200을 반환한다")
  void getReportDetail() throws Exception {
    AdminReportDetailResDto.ReportInfo reportInfo =
        new AdminReportDetailResDto.ReportInfo(
            REPORT_ID,
            USER_ID,
            ReportReasonCode.NOT_ON_SITE,
            "현장 아님",
            ReportStatus.OPEN,
            OffsetDateTime.now(),
            null,
            null,
            null,
            0L);
    AdminReportDetailResDto.TargetInfo targetInfo =
        new AdminReportDetailResDto.TargetInfo(
            PARTICIPATION_ID,
            EVENT_ID,
            UUID.randomUUID(),
            USER_ID,
            "media/key.jpg",
            "참여 내용",
            ParticipationStatus.SUCCESS,
            UUID.randomUUID());
    AdminReportDetailResDto detail = new AdminReportDetailResDto(reportInfo, targetInfo, List.of());

    when(adminReportService.getReportDetail(any(), eq(REPORT_ID))).thenReturn(detail);

    mockMvc
        .perform(get("/admin/zone-event-reports/{reportId}", REPORT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.report.reportId").value(REPORT_ID.toString()))
        .andExpect(jsonPath("$.data.target.participationId").value(PARTICIPATION_ID.toString()));
  }

  @Test
  @DisplayName("존재하지 않는 신고 상세 조회는 404를 반환한다")
  void getReportDetailNotFound() throws Exception {
    when(adminReportService.getReportDetail(any(), eq(REPORT_ID)))
        .thenThrow(new ResourceNotFoundException("error.zone_event.report.not_found"));

    mockMvc
        .perform(get("/admin/zone-event-reports/{reportId}", REPORT_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("신고를 찾을 수 없습니다."));
  }

  @Test
  @DisplayName("신고 인정 처리는 200을 반환한다")
  void upholdReport() throws Exception {
    AdminReportDetailResDto.ReportInfo reportInfo =
        new AdminReportDetailResDto.ReportInfo(
            REPORT_ID,
            USER_ID,
            ReportReasonCode.NOT_ON_SITE,
            "현장 아님",
            ReportStatus.UPHELD,
            OffsetDateTime.now(),
            USER_ID,
            OffsetDateTime.now(),
            "인정",
            1L);
    AdminReportDetailResDto.TargetInfo targetInfo =
        new AdminReportDetailResDto.TargetInfo(
            PARTICIPATION_ID,
            EVENT_ID,
            UUID.randomUUID(),
            USER_ID,
            "media/key.jpg",
            "참여 내용",
            ParticipationStatus.SUCCESS,
            UUID.randomUUID());
    AdminReportDetailResDto detail = new AdminReportDetailResDto(reportInfo, targetInfo, List.of());

    when(adminReportService.upholdReport(any(), eq(REPORT_ID), any())).thenReturn(detail);

    mockMvc
        .perform(
            post("/admin/zone-event-reports/{reportId}/uphold", REPORT_ID)
                .contentType("application/json")
                .content("{\"note\":\"인정\",\"action\":\"HOLD\",\"expectedRevision\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.report.status").value("UPHELD"));
  }

  @Test
  @DisplayName("신고 기각 처리는 200을 반환한다")
  void dismissReport() throws Exception {
    AdminReportDetailResDto.ReportInfo reportInfo =
        new AdminReportDetailResDto.ReportInfo(
            REPORT_ID,
            USER_ID,
            ReportReasonCode.NOT_ON_SITE,
            "현장 아님",
            ReportStatus.DISMISSED,
            OffsetDateTime.now(),
            USER_ID,
            OffsetDateTime.now(),
            "기각",
            1L);
    AdminReportDetailResDto.TargetInfo targetInfo =
        new AdminReportDetailResDto.TargetInfo(
            PARTICIPATION_ID,
            EVENT_ID,
            UUID.randomUUID(),
            USER_ID,
            "media/key.jpg",
            "참여 내용",
            ParticipationStatus.SUCCESS,
            UUID.randomUUID());
    AdminReportDetailResDto detail = new AdminReportDetailResDto(reportInfo, targetInfo, List.of());

    when(adminReportService.dismissReport(any(), eq(REPORT_ID), any())).thenReturn(detail);

    mockMvc
        .perform(
            post("/admin/zone-event-reports/{reportId}/dismiss", REPORT_ID)
                .contentType("application/json")
                .content("{\"note\":\"기각\",\"expectedRevision\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.report.status").value("DISMISSED"));
  }

  @Test
  @DisplayName("리비전 충돌 시 409를 반환한다")
  void revisionConflict() throws Exception {
    when(adminReportService.upholdReport(any(), eq(REPORT_ID), any()))
        .thenThrow(new ConflictException("error.revision.conflict"));

    mockMvc
        .perform(
            post("/admin/zone-event-reports/{reportId}/uphold", REPORT_ID)
                .contentType("application/json")
                .content("{\"note\":\"인정\",\"action\":\"HOLD\",\"expectedRevision\":99}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("데이터가 이미 변경되었습니다."));
  }

  @Test
  @DisplayName("운영자 권한이 없으면 403을 반환한다")
  void forbidden() throws Exception {
    when(adminReportService.getReports(any(), any(), any(), any(), any(), any()))
        .thenThrow(new ForbiddenException("error.operator.forbidden"));

    mockMvc
        .perform(get("/admin/zone-event-reports"))
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

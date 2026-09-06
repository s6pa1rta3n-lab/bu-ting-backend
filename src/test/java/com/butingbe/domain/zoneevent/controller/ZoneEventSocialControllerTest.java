package com.butingbe.domain.zoneevent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.request.CommentCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.CommentUpdateReqDto;
import com.butingbe.domain.zoneevent.dto.request.ReportCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.VisibilityUpdateReqDto;
import com.butingbe.domain.zoneevent.dto.response.CommentResDto;
import com.butingbe.domain.zoneevent.dto.response.ReportResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.service.ZoneEventSocialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class ZoneEventSocialControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID PARTICIPATION_ID =
      UUID.fromString("22222222-0000-0000-0000-000000000002");

  @Mock private ZoneEventSocialService socialService;
  @InjectMocks private ZoneEventSocialController controller;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authenticatedUserResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
  }

  @Test
  @DisplayName("likeParticipation triggers like action")
  void likeParticipation() throws Exception {
    mockMvc
        .perform(post("/zone-events/participations/{id}/likes", PARTICIPATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(socialService).likeParticipation(USER_ID, PARTICIPATION_ID);
  }

  @Test
  @DisplayName("unlikeParticipation triggers unlike action")
  void unlikeParticipation() throws Exception {
    mockMvc
        .perform(delete("/zone-events/participations/{id}/likes", PARTICIPATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(socialService).unlikeParticipation(USER_ID, PARTICIPATION_ID);
  }

  @Test
  @DisplayName("addComment creates new comment")
  void addComment() throws Exception {
    UUID commentId = UUID.randomUUID();
    CommentCreateReqDto req = new CommentCreateReqDto("댓글 내용");
    CommentResDto res =
        new CommentResDto(
            commentId,
            PARTICIPATION_ID,
            USER_ID,
            "닉네임",
            null,
            null,
            "댓글 내용",
            LocalDateTime.now(),
            true);

    when(socialService.addComment(eq(USER_ID), eq(PARTICIPATION_ID), eq("댓글 내용"))).thenReturn(res);

    mockMvc
        .perform(
            post("/zone-events/participations/{id}/comments", PARTICIPATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.commentId").value(commentId.toString()))
        .andExpect(jsonPath("$.data.content").value("댓글 내용"));
  }

  @Test
  @DisplayName("deleteComment deletes specified comment")
  void deleteComment() throws Exception {
    UUID commentId = UUID.randomUUID();
    mockMvc
        .perform(delete("/zone-events/comments/{commentId}", commentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(socialService).deleteComment(eq(USER_ID), eq(commentId), any());
  }

  @Test
  @DisplayName("getComments returns comments list")
  void getComments() throws Exception {
    when(socialService.getComments(eq(USER_ID), eq(PARTICIPATION_ID))).thenReturn(List.of());

    mockMvc
        .perform(get("/zone-events/participations/{id}/comments", PARTICIPATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @DisplayName("updateComment updates comment content")
  void updateComment() throws Exception {
    UUID commentId = UUID.randomUUID();
    CommentUpdateReqDto req = new CommentUpdateReqDto("수정된 댓글");
    CommentResDto res =
        new CommentResDto(
            commentId,
            PARTICIPATION_ID,
            USER_ID,
            "닉네임",
            null,
            null,
            "수정된 댓글",
            LocalDateTime.now(),
            true);

    when(socialService.updateComment(eq(USER_ID), eq(commentId), eq("수정된 댓글"), any()))
        .thenReturn(res);

    mockMvc
        .perform(
            patch("/zone-events/comments/{commentId}", commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").value("수정된 댓글"));
  }

  @Test
  @DisplayName("reportParticipation creates a report")
  void reportParticipation() throws Exception {
    UUID reportId = UUID.randomUUID();
    ReportCreateReqDto req = new ReportCreateReqDto("SPAM", "스팸 내용");
    ReportResDto res =
        new ReportResDto(
            reportId,
            PARTICIPATION_ID,
            USER_ID,
            "SPAM",
            "스팸 내용",
            com.butingbe.domain.zoneevent.entity.ReportStatus.OPEN,
            LocalDateTime.now());

    when(socialService.reportParticipation(
            eq(USER_ID), eq(PARTICIPATION_ID), eq("SPAM"), eq("스팸 내용")))
        .thenReturn(res);

    mockMvc
        .perform(
            post("/zone-events/participations/{id}/reports", PARTICIPATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.reportId").value(reportId.toString()));
  }

  @Test
  @DisplayName("updateVisibility changes visibility")
  void updateVisibility() throws Exception {
    VisibilityUpdateReqDto req = new VisibilityUpdateReqDto(ParticipationVisibility.PRIVATE);

    mockMvc
        .perform(
            patch("/zone-events/participations/{id}/visibility", PARTICIPATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(socialService)
        .updateVisibility(
            eq(USER_ID), eq(PARTICIPATION_ID), eq(ParticipationVisibility.PRIVATE), any());
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
        return new AuthenticatedUser(USER_ID, "u@example.com", "u", List.of());
      }
    };
  }
}

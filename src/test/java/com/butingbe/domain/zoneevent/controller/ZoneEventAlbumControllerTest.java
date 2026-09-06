package com.butingbe.domain.zoneevent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.request.AlbumSort;
import com.butingbe.domain.zoneevent.dto.response.AlbumPageResDto;
import com.butingbe.domain.zoneevent.service.ZoneEventAlbumService;
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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class ZoneEventAlbumControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID EVENT_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");
  private static final UUID ROUND_ID = UUID.fromString("33333333-0000-0000-0000-000000000003");

  @Mock private ZoneEventAlbumService albumService;
  @InjectMocks private ZoneEventAlbumController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authenticatedUserResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
  }

  @Test
  @DisplayName("getEventAlbum returns paginated feed for event")
  void getEventAlbum() throws Exception {
    when(albumService.getEventAlbum(eq(EVENT_ID), any(), eq(20), eq(AlbumSort.LATEST), eq(USER_ID)))
        .thenReturn(new AlbumPageResDto(List.of(), "cursor123", true));

    mockMvc
        .perform(get("/zone-events/{eventId}/album", EVENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nextCursor").value("cursor123"));
  }

  @Test
  @DisplayName("getZoneAlbum returns paginated feed for zone")
  void getZoneAlbum() throws Exception {
    when(albumService.getZoneAlbum(
            eq("GWANGAN"), any(), eq(20), eq(AlbumSort.MOST_LIKED), eq(USER_ID)))
        .thenReturn(new AlbumPageResDto(List.of(), null, false));

    mockMvc
        .perform(get("/zone-events/zones/{zoneId}/album", "GWANGAN").param("sort", "MOST_LIKED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  @DisplayName("getRoundAlbum returns paginated feed for round")
  void getRoundAlbum() throws Exception {
    when(albumService.getRoundAlbum(eq(ROUND_ID), any(), eq(20), eq(AlbumSort.LATEST), eq(USER_ID)))
        .thenReturn(new AlbumPageResDto(List.of(), null, false));

    mockMvc
        .perform(get("/zone-events/rounds/{roundId}/album", ROUND_ID))
        .andExpect(status().isOk())
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
        return new AuthenticatedUser(USER_ID, "u@example.com", "u", List.of());
      }
    };
  }
}

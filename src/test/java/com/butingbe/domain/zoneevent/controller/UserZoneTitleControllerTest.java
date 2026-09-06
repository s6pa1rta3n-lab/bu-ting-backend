package com.butingbe.domain.zoneevent.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.response.UserTitlesSummaryResDto;
import com.butingbe.domain.zoneevent.dto.response.UserZoneTitleResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneTitleDefResDto;
import com.butingbe.domain.zoneevent.service.ZoneTitleService;
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
class UserZoneTitleControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID USER_TITLE_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");

  @Mock private ZoneTitleService zoneTitleService;
  @InjectMocks private UserZoneTitleController controller;

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
  @DisplayName("getMyTitles returns user title summary")
  void getMyTitles() throws Exception {
    when(zoneTitleService.getUserTitlesSummary(USER_ID))
        .thenReturn(
            new UserTitlesSummaryResDto(
                null,
                com.butingbe.domain.zoneevent.entity.CityGrade.BEGINNER,
                List.of(),
                List.of()));

    mockMvc
        .perform(get("/users/me/titles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @DisplayName("equipTitle equips the specified title")
  void equipTitle() throws Exception {
    ZoneTitleDefResDto def =
        new ZoneTitleDefResDto(
            UUID.randomUUID().toString(),
            "GWANGAN",
            1,
            3,
            "TITLE_GWANGAN_EXPLORER",
            "광안리 탐험가",
            "BADGE",
            "#000000");
    UserZoneTitleResDto res =
        new UserZoneTitleResDto(
            USER_TITLE_ID.toString(), def, true, java.time.OffsetDateTime.now());
    when(zoneTitleService.equipTitle(USER_ID, USER_TITLE_ID)).thenReturn(res);

    mockMvc
        .perform(patch("/users/me/titles/{userTitleId}/equip", USER_TITLE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.titleDef.titleCode").value("TITLE_GWANGAN_EXPLORER"));
  }

  @Test
  @DisplayName("unequipTitle removes equipped title")
  void unequipTitle() throws Exception {
    mockMvc
        .perform(patch("/users/me/titles/unequip"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(zoneTitleService).unequipTitle(USER_ID);
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

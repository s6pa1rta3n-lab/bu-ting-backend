package com.butingbe.domain.zoneevent.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.butingbe.domain.zoneevent.dto.response.PublicCurrentRoundResDto;
import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.RoundType;
import com.butingbe.domain.zoneevent.service.AdminZoneEventRoundService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ZoneEventRoundPublicControllerTest {

  private static final UUID ROUND_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

  @Mock private AdminZoneEventRoundService roundService;
  @InjectMocks private ZoneEventRoundPublicController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
  }

  @Test
  @DisplayName("getCurrentRound returns current active round")
  void getCurrentRound() throws Exception {
    PublicCurrentRoundResDto res =
        new PublicCurrentRoundResDto(
            ROUND_ID,
            RoundType.REGULAR,
            OffsetDateTime.now(),
            OffsetDateTime.now().plusDays(7),
            RoundStatus.OPEN,
            List.of());

    when(roundService.getCurrentActiveRound()).thenReturn(res);

    mockMvc
        .perform(get("/zone-events/rounds/current"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.roundId").value(ROUND_ID.toString()));
  }
}

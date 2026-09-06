package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.butingbe.domain.zoneevent.dto.response.EquippedTitleResDto;
import com.butingbe.domain.zoneevent.dto.response.UserTitlesSummaryResDto;
import com.butingbe.domain.zoneevent.dto.response.UserZoneTitleResDto;
import com.butingbe.domain.zoneevent.entity.CityGrade;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.UserZoneTitle;
import com.butingbe.domain.zoneevent.entity.ZoneTitleDef;
import com.butingbe.domain.zoneevent.repository.UserZoneTitleRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneTitleDefRepository;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ZoneTitleServiceTest {

  private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
  private static final UUID OTHER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
  private static final String ZONE_ID = "GWANGAN";

  @Mock private ZoneTitleDefRepository titleDefRepository;
  @Mock private UserZoneTitleRepository userZoneTitleRepository;
  @Mock private ZoneEventParticipationRepository participationRepository;
  @Mock private CityGradeService cityGradeService;

  @InjectMocks private ZoneTitleService zoneTitleService;

  private ZoneTitleDef makeDef(UUID id, int tier, int req) {
    ZoneTitleDef def =
        ZoneTitleDef.builder()
            .titleCode("GWANGAN_T" + tier)
            .zoneId(ZONE_ID)
            .tier(tier)
            .titleName("광안 " + tier + "티어")
            .requiredSuccessCount(req)
            .build();
    ReflectionTestUtils.setField(def, "id", id);
    return def;
  }

  @Test
  @DisplayName("Awards eligible title and auto-equips when no title is equipped")
  void evaluateAndAwardTitles_autoEquip() {
    UUID defId = UUID.randomUUID();
    ZoneTitleDef def1 = makeDef(defId, 1, 1);

    when(participationRepository.countByUserIdAndEvent_ZoneIdAndStatus(
            USER_ID, ZONE_ID, ParticipationStatus.SUCCESS))
        .thenReturn(1L);
    when(titleDefRepository.findByZoneIdOrderByTierAsc(ZONE_ID)).thenReturn(List.of(def1));
    when(userZoneTitleRepository.findByUserId(USER_ID)).thenReturn(List.of());
    when(userZoneTitleRepository.save(any(UserZoneTitle.class)))
        .thenAnswer(
            inv -> {
              UserZoneTitle t = inv.getArgument(0);
              ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
              return t;
            });

    List<UserZoneTitleResDto> awarded = zoneTitleService.evaluateAndAwardTitles(USER_ID, ZONE_ID);

    assertThat(awarded).hasSize(1);
    assertThat(awarded.get(0).isEquipped()).isTrue();
    verify(userZoneTitleRepository).save(any(UserZoneTitle.class));
    verify(cityGradeService).evaluateAndUpgradeGrade(USER_ID);
  }

  @Test
  @DisplayName("Does not duplicate already owned titles")
  void evaluateAndAwardTitles_noDuplicates() {
    UUID defId = UUID.randomUUID();
    ZoneTitleDef def1 = makeDef(defId, 1, 1);
    UserZoneTitle existing =
        UserZoneTitle.builder()
            .userId(USER_ID)
            .titleDef(def1)
            .isEquipped(true)
            .earnedAt(OffsetDateTime.now())
            .build();

    when(participationRepository.countByUserIdAndEvent_ZoneIdAndStatus(
            USER_ID, ZONE_ID, ParticipationStatus.SUCCESS))
        .thenReturn(2L);
    when(titleDefRepository.findByZoneIdOrderByTierAsc(ZONE_ID)).thenReturn(List.of(def1));
    when(userZoneTitleRepository.findByUserId(USER_ID)).thenReturn(List.of(existing));

    List<UserZoneTitleResDto> awarded = zoneTitleService.evaluateAndAwardTitles(USER_ID, ZONE_ID);

    assertThat(awarded).isEmpty();
    verify(userZoneTitleRepository, never()).save(any(UserZoneTitle.class));
    verify(cityGradeService, never()).evaluateAndUpgradeGrade(any());
  }

  @Test
  @DisplayName("Equips title and unequips previously equipped title")
  void equipTitle_success() {
    UUID title1Id = UUID.randomUUID();
    UUID title2Id = UUID.randomUUID();
    ZoneTitleDef def1 = makeDef(UUID.randomUUID(), 1, 1);
    ZoneTitleDef def2 = makeDef(UUID.randomUUID(), 2, 3);

    UserZoneTitle title1 =
        UserZoneTitle.builder().userId(USER_ID).titleDef(def1).isEquipped(true).build();
    UserZoneTitle title2 =
        UserZoneTitle.builder().userId(USER_ID).titleDef(def2).isEquipped(false).build();
    ReflectionTestUtils.setField(title2, "id", title2Id);

    when(userZoneTitleRepository.findById(title2Id)).thenReturn(Optional.of(title2));
    when(userZoneTitleRepository.findByUserIdAndIsEquippedTrue(USER_ID))
        .thenReturn(Optional.of(title1));

    UserZoneTitleResDto result = zoneTitleService.equipTitle(USER_ID, title2Id);

    assertThat(result.isEquipped()).isTrue();
    assertThat(title1.getIsEquipped()).isFalse();
    assertThat(title2.getIsEquipped()).isTrue();
  }

  @Test
  @DisplayName("equipTitle throws ResourceNotFoundException when title not found")
  void equipTitle_notFound() {
    UUID nonExistent = UUID.randomUUID();
    when(userZoneTitleRepository.findById(nonExistent)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> zoneTitleService.equipTitle(USER_ID, nonExistent))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("equipTitle throws ForbiddenException when title belongs to another user")
  void equipTitle_forbidden() {
    UUID titleId = UUID.randomUUID();
    ZoneTitleDef def = makeDef(UUID.randomUUID(), 1, 1);
    UserZoneTitle title =
        UserZoneTitle.builder().userId(OTHER_ID).titleDef(def).isEquipped(false).build();

    when(userZoneTitleRepository.findById(titleId)).thenReturn(Optional.of(title));

    assertThatThrownBy(() -> zoneTitleService.equipTitle(USER_ID, titleId))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("unequipTitle unequips equipped title")
  void unequipTitle_success() {
    ZoneTitleDef def = makeDef(UUID.randomUUID(), 1, 1);
    UserZoneTitle title =
        UserZoneTitle.builder().userId(USER_ID).titleDef(def).isEquipped(true).build();

    when(userZoneTitleRepository.findByUserIdAndIsEquippedTrue(USER_ID))
        .thenReturn(Optional.of(title));

    zoneTitleService.unequipTitle(USER_ID);

    assertThat(title.getIsEquipped()).isFalse();
  }

  @Test
  @DisplayName("getEquippedTitle returns mapped dto or null")
  void getEquippedTitle_test() {
    when(userZoneTitleRepository.findByUserIdAndIsEquippedTrue(USER_ID))
        .thenReturn(Optional.empty());
    assertThat(zoneTitleService.getEquippedTitle(USER_ID)).isNull();

    ZoneTitleDef def = makeDef(UUID.randomUUID(), 1, 1);
    UserZoneTitle title =
        UserZoneTitle.builder().userId(USER_ID).titleDef(def).isEquipped(true).build();
    when(userZoneTitleRepository.findByUserIdAndIsEquippedTrue(USER_ID))
        .thenReturn(Optional.of(title));

    EquippedTitleResDto dto = zoneTitleService.getEquippedTitle(USER_ID);
    assertThat(dto).isNotNull();
    assertThat(dto.titleCode()).isEqualTo("GWANGAN_T1");
  }

  @Test
  @DisplayName("getUserTitlesSummary returns aggregated titles, grade, and zone progression")
  void getUserTitlesSummary_success() {
    ZoneTitleDef def1 = makeDef(UUID.randomUUID(), 1, 2);
    ZoneTitleDef def2 = makeDef(UUID.randomUUID(), 2, 5);
    UserZoneTitle earned =
        UserZoneTitle.builder().userId(USER_ID).titleDef(def1).isEquipped(true).build();
    ReflectionTestUtils.setField(earned, "id", UUID.randomUUID());

    when(userZoneTitleRepository.findByUserIdAndIsEquippedTrue(USER_ID))
        .thenReturn(Optional.of(earned));
    when(cityGradeService.getCurrentGrade(USER_ID)).thenReturn(CityGrade.BEGINNER);
    when(userZoneTitleRepository.findByUserId(USER_ID)).thenReturn(List.of(earned));
    when(titleDefRepository.findByZoneIdOrderByTierAsc(anyString()))
        .thenReturn(List.of(def1, def2));
    when(participationRepository.countByUserIdAndEvent_ZoneIdAndStatus(
            eq(USER_ID), anyString(), any()))
        .thenReturn(3L);

    UserTitlesSummaryResDto summary = zoneTitleService.getUserTitlesSummary(USER_ID);

    assertThat(summary.cityGrade()).isEqualTo(CityGrade.BEGINNER);
    assertThat(summary.earnedTitles()).hasSize(1);
    assertThat(summary.zoneProgressList()).isNotEmpty();
    assertThat(summary.zoneProgressList().get(0).currentTier()).isEqualTo(1);
    assertThat(summary.zoneProgressList().get(0).nextTierRequiredCount()).isEqualTo(5);
  }
}

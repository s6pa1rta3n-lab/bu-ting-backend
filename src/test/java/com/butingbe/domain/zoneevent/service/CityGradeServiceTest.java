package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.butingbe.domain.zoneevent.entity.CityGrade;
import com.butingbe.domain.zoneevent.entity.UserCityGradeHistory;
import com.butingbe.domain.zoneevent.entity.UserZoneTitle;
import com.butingbe.domain.zoneevent.entity.ZoneTitleDef;
import com.butingbe.domain.zoneevent.repository.UserCityGradeHistoryRepository;
import com.butingbe.domain.zoneevent.repository.UserZoneTitleRepository;
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

@ExtendWith(MockitoExtension.class)
class CityGradeServiceTest {

  private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

  @Mock private UserZoneTitleRepository userZoneTitleRepository;
  @Mock private UserCityGradeHistoryRepository cityGradeHistoryRepository;

  @InjectMocks private CityGradeService cityGradeService;

  private ZoneTitleDef makeDef(String zoneId, int tier) {
    return ZoneTitleDef.builder()
        .titleCode("TITLE_" + zoneId + "_" + tier)
        .zoneId(zoneId)
        .tier(tier)
        .titleName("Name " + zoneId + " " + tier)
        .requiredSuccessCount(tier * 3)
        .build();
  }

  private UserZoneTitle makeTitle(String zoneId, int tier) {
    return UserZoneTitle.builder()
        .userId(USER_ID)
        .titleDef(makeDef(zoneId, tier))
        .isEquipped(false)
        .build();
  }

  @Test
  @DisplayName("Empty titles evaluate to BEGINNER")
  void computeGrade_empty() {
    CityGrade grade = cityGradeService.computeGradeFromTitles(List.of());
    assertThat(grade).isEqualTo(CityGrade.BEGINNER);

    CityGrade nullGrade = cityGradeService.computeGradeFromTitles(null);
    assertThat(nullGrade).isEqualTo(CityGrade.BEGINNER);
  }

  @Test
  @DisplayName("Tier 1 titles in 3 zones qualify for EXPLORER")
  void computeGrade_explorer() {
    List<UserZoneTitle> titles =
        List.of(makeTitle("ZONE_A", 1), makeTitle("ZONE_B", 1), makeTitle("ZONE_C", 1));
    CityGrade grade = cityGradeService.computeGradeFromTitles(titles);
    assertThat(grade).isEqualTo(CityGrade.EXPLORER);
  }

  @Test
  @DisplayName("Tier 3 titles in 2 zones qualify for MASTER")
  void computeGrade_master() {
    List<UserZoneTitle> titles =
        List.of(makeTitle("ZONE_A", 3), makeTitle("ZONE_B", 3), makeTitle("ZONE_C", 1));
    CityGrade grade = cityGradeService.computeGradeFromTitles(titles);
    assertThat(grade).isEqualTo(CityGrade.MASTER);
  }

  @Test
  @DisplayName("Tier 2 titles in 6 zones qualify for TRUE_BUSAN")
  void computeGrade_trueBusan_tier2() {
    List<UserZoneTitle> titles =
        List.of(
            makeTitle("ZONE_A", 2),
            makeTitle("ZONE_B", 2),
            makeTitle("ZONE_C", 2),
            makeTitle("ZONE_D", 2),
            makeTitle("ZONE_E", 2),
            makeTitle("ZONE_F", 2));
    CityGrade grade = cityGradeService.computeGradeFromTitles(titles);
    assertThat(grade).isEqualTo(CityGrade.TRUE_BUSAN);
  }

  @Test
  @DisplayName("Tier 3 titles in 4 zones qualify for TRUE_BUSAN")
  void computeGrade_trueBusan_tier3() {
    List<UserZoneTitle> titles =
        List.of(
            makeTitle("ZONE_A", 3),
            makeTitle("ZONE_B", 3),
            makeTitle("ZONE_C", 3),
            makeTitle("ZONE_D", 3));
    CityGrade grade = cityGradeService.computeGradeFromTitles(titles);
    assertThat(grade).isEqualTo(CityGrade.TRUE_BUSAN);
  }

  @Test
  @DisplayName("First evaluation saves history when no history exists")
  void evaluateAndUpgradeGrade_initial() {
    List<UserZoneTitle> titles =
        List.of(makeTitle("ZONE_A", 1), makeTitle("ZONE_B", 1), makeTitle("ZONE_C", 1));
    when(userZoneTitleRepository.findByUserId(USER_ID)).thenReturn(titles);
    when(cityGradeHistoryRepository.findFirstByUserIdOrderByReachedAtDesc(USER_ID))
        .thenReturn(Optional.empty());

    CityGrade grade = cityGradeService.evaluateAndUpgradeGrade(USER_ID);

    assertThat(grade).isEqualTo(CityGrade.EXPLORER);
    verify(cityGradeHistoryRepository).save(any(UserCityGradeHistory.class));
  }

  @Test
  @DisplayName("Upgrade to higher grade records new history")
  void evaluateAndUpgradeGrade_upgrade() {
    List<UserZoneTitle> titles = List.of(makeTitle("ZONE_A", 3), makeTitle("ZONE_B", 3));
    when(userZoneTitleRepository.findByUserId(USER_ID)).thenReturn(titles);

    UserCityGradeHistory oldHistory =
        UserCityGradeHistory.builder()
            .userId(USER_ID)
            .grade(CityGrade.EXPLORER)
            .reachedAt(OffsetDateTime.now().minusDays(1))
            .build();
    when(cityGradeHistoryRepository.findFirstByUserIdOrderByReachedAtDesc(USER_ID))
        .thenReturn(Optional.of(oldHistory));

    CityGrade grade = cityGradeService.evaluateAndUpgradeGrade(USER_ID);

    assertThat(grade).isEqualTo(CityGrade.MASTER);
    verify(cityGradeHistoryRepository).save(any(UserCityGradeHistory.class));
  }

  @Test
  @DisplayName("No upgrade when computed grade is lower or equal to current highest")
  void evaluateAndUpgradeGrade_noUpgrade() {
    List<UserZoneTitle> titles = List.of(makeTitle("ZONE_A", 1));
    when(userZoneTitleRepository.findByUserId(USER_ID)).thenReturn(titles);

    UserCityGradeHistory oldHistory =
        UserCityGradeHistory.builder()
            .userId(USER_ID)
            .grade(CityGrade.MASTER)
            .reachedAt(OffsetDateTime.now().minusDays(1))
            .build();
    when(cityGradeHistoryRepository.findFirstByUserIdOrderByReachedAtDesc(USER_ID))
        .thenReturn(Optional.of(oldHistory));

    CityGrade grade = cityGradeService.evaluateAndUpgradeGrade(USER_ID);

    assertThat(grade).isEqualTo(CityGrade.MASTER);
    verify(cityGradeHistoryRepository, never()).save(any(UserCityGradeHistory.class));
  }

  @Test
  @DisplayName("getCurrentGrade returns BEGINNER when no history exists")
  void getCurrentGrade_defaultBeginner() {
    when(cityGradeHistoryRepository.findFirstByUserIdOrderByReachedAtDesc(USER_ID))
        .thenReturn(Optional.empty());

    CityGrade grade = cityGradeService.getCurrentGrade(USER_ID);

    assertThat(grade).isEqualTo(CityGrade.BEGINNER);
  }

  @Test
  @DisplayName("getCurrentGrade returns recorded grade when history exists")
  void getCurrentGrade_existing() {
    UserCityGradeHistory history =
        UserCityGradeHistory.builder()
            .userId(USER_ID)
            .grade(CityGrade.EXPLORER)
            .reachedAt(OffsetDateTime.now())
            .build();
    when(cityGradeHistoryRepository.findFirstByUserIdOrderByReachedAtDesc(USER_ID))
        .thenReturn(Optional.of(history));

    CityGrade grade = cityGradeService.getCurrentGrade(USER_ID);

    assertThat(grade).isEqualTo(CityGrade.EXPLORER);
  }
}

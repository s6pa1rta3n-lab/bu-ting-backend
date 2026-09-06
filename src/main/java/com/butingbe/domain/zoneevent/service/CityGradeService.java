package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.zoneevent.entity.CityGrade;
import com.butingbe.domain.zoneevent.entity.UserCityGradeHistory;
import com.butingbe.domain.zoneevent.entity.UserZoneTitle;
import com.butingbe.domain.zoneevent.repository.UserCityGradeHistoryRepository;
import com.butingbe.domain.zoneevent.repository.UserZoneTitleRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Evaluates and tracks user progression across city-wide grades. */
@Service
@RequiredArgsConstructor
public class CityGradeService {

  private final UserZoneTitleRepository userZoneTitleRepository;
  private final UserCityGradeHistoryRepository cityGradeHistoryRepository;

  /**
   * Evaluates user's current achievements and upgrades city grade if eligible.
   *
   * @param userId user ID
   * @return current active city grade
   */
  @Transactional
  public CityGrade evaluateAndUpgradeGrade(UUID userId) {
    List<UserZoneTitle> titles = userZoneTitleRepository.findByUserId(userId);
    CityGrade calculatedGrade = computeGradeFromTitles(titles);

    Optional<UserCityGradeHistory> latestHistory =
        cityGradeHistoryRepository.findFirstByUserIdOrderByReachedAtDesc(userId);

    if (latestHistory.isEmpty()) {
      cityGradeHistoryRepository.save(
          UserCityGradeHistory.builder()
              .userId(userId)
              .grade(calculatedGrade)
              .reachedAt(OffsetDateTime.now())
              .build());
      return calculatedGrade;
    }

    CityGrade currentHighest = latestHistory.get().getGrade();
    if (calculatedGrade.isHigherThan(currentHighest)) {
      cityGradeHistoryRepository.save(
          UserCityGradeHistory.builder()
              .userId(userId)
              .grade(calculatedGrade)
              .reachedAt(OffsetDateTime.now())
              .build());
      return calculatedGrade;
    }

    return currentHighest;
  }

  /**
   * Retrieves the current city grade of a user.
   *
   * @param userId user ID
   * @return current active city grade
   */
  @Transactional(readOnly = true)
  public CityGrade getCurrentGrade(UUID userId) {
    return cityGradeHistoryRepository
        .findFirstByUserIdOrderByReachedAtDesc(userId)
        .map(UserCityGradeHistory::getGrade)
        .orElse(CityGrade.BEGINNER);
  }

  /**
   * Computes city grade from owned titles based on zone tier coverage.
   *
   * @param titles user earned titles
   * @return calculated city grade
   */
  public CityGrade computeGradeFromTitles(List<UserZoneTitle> titles) {
    if (titles == null || titles.isEmpty()) {
      return CityGrade.BEGINNER;
    }

    Map<String, Integer> maxTierPerZone = new HashMap<>();
    for (UserZoneTitle title : titles) {
      String zoneId = title.getTitleDef().getZoneId();
      int tier = title.getTitleDef().getTier();
      maxTierPerZone.merge(zoneId, tier, Math::max);
    }

    long tier1Count = maxTierPerZone.values().stream().filter(tier -> tier >= 1).count();
    long tier2Count = maxTierPerZone.values().stream().filter(tier -> tier >= 2).count();
    long tier3Count = maxTierPerZone.values().stream().filter(tier -> tier >= 3).count();

    if (tier2Count >= 6 || tier3Count >= 4) {
      return CityGrade.TRUE_BUSAN;
    }
    if (tier3Count >= 2) {
      return CityGrade.MASTER;
    }
    if (tier1Count >= 3) {
      return CityGrade.EXPLORER;
    }
    return CityGrade.BEGINNER;
  }
}

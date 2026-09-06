package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.chat.entity.ChatZone;
import com.butingbe.domain.zoneevent.dto.response.EquippedTitleResDto;
import com.butingbe.domain.zoneevent.dto.response.UserTitlesSummaryResDto;
import com.butingbe.domain.zoneevent.dto.response.UserZoneTitleResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneProgressResDto;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages zone title unlock progression, equipping titles, and user title inventories. */
@Service
@RequiredArgsConstructor
public class ZoneTitleService {

  private final ZoneTitleDefRepository titleDefRepository;
  private final UserZoneTitleRepository userZoneTitleRepository;
  private final ZoneEventParticipationRepository participationRepository;
  private final CityGradeService cityGradeService;

  /**
   * Evaluates if user qualifies for new zone titles and awards them.
   *
   * @param userId user ID
   * @param zoneId event zone ID
   * @return list of newly unlocked titles
   */
  @Transactional
  public List<UserZoneTitleResDto> evaluateAndAwardTitles(UUID userId, String zoneId) {
    long successCount =
        participationRepository.countByUserIdAndEvent_ZoneIdAndStatus(
            userId, zoneId, ParticipationStatus.SUCCESS);

    List<ZoneTitleDef> defs = titleDefRepository.findByZoneIdOrderByTierAsc(zoneId);
    List<UserZoneTitle> existingTitles = userZoneTitleRepository.findByUserId(userId);
    Set<UUID> ownedDefIds =
        existingTitles.stream().map(ut -> ut.getTitleDef().getId()).collect(Collectors.toSet());
    boolean hasEquipped = existingTitles.stream().anyMatch(UserZoneTitle::getIsEquipped);

    List<UserZoneTitleResDto> newlyEarned = new ArrayList<>();

    for (ZoneTitleDef def : defs) {
      if (successCount >= def.getRequiredSuccessCount() && !ownedDefIds.contains(def.getId())) {
        boolean autoEquip = !hasEquipped;
        UserZoneTitle newTitle =
            UserZoneTitle.builder()
                .userId(userId)
                .titleDef(def)
                .isEquipped(autoEquip)
                .earnedAt(OffsetDateTime.now())
                .build();
        userZoneTitleRepository.save(newTitle);
        ownedDefIds.add(def.getId());
        if (autoEquip) {
          hasEquipped = true;
        }
        newlyEarned.add(UserZoneTitleResDto.from(newTitle));
      }
    }

    if (!newlyEarned.isEmpty()) {
      cityGradeService.evaluateAndUpgradeGrade(userId);
    }

    return newlyEarned;
  }

  /**
   * Equips a user zone title.
   *
   * @param userId user ID
   * @param userTitleId user zone title ID
   * @return equipped user zone title
   */
  @Transactional
  public UserZoneTitleResDto equipTitle(UUID userId, UUID userTitleId) {
    UserZoneTitle titleToEquip =
        userZoneTitleRepository
            .findById(userTitleId)
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));

    if (!titleToEquip.getUserId().equals(userId)) {
      throw new ForbiddenException("error.zone_event.participation.forbidden");
    }

    userZoneTitleRepository.findByUserIdAndIsEquippedTrue(userId).ifPresent(UserZoneTitle::unequip);

    titleToEquip.equip();
    return UserZoneTitleResDto.from(titleToEquip);
  }

  /**
   * Unequips currently equipped user zone title.
   *
   * @param userId user ID
   */
  @Transactional
  public void unequipTitle(UUID userId) {
    userZoneTitleRepository.findByUserIdAndIsEquippedTrue(userId).ifPresent(UserZoneTitle::unequip);
  }

  /**
   * Retrieves user's currently equipped title metadata.
   *
   * @param userId user ID
   * @return equipped title metadata or null
   */
  @Transactional(readOnly = true)
  public EquippedTitleResDto getEquippedTitle(UUID userId) {
    return userZoneTitleRepository
        .findByUserIdAndIsEquippedTrue(userId)
        .map(ut -> EquippedTitleResDto.from(ut.getTitleDef()))
        .orElse(null);
  }

  /**
   * Retrieves complete summary of user titles, equipped title, city grade, and zone progression.
   *
   * @param userId user ID
   * @return title summary
   */
  @Transactional(readOnly = true)
  public UserTitlesSummaryResDto getUserTitlesSummary(UUID userId) {
    EquippedTitleResDto equipped = getEquippedTitle(userId);
    CityGrade cityGrade = cityGradeService.getCurrentGrade(userId);
    List<UserZoneTitle> earned = userZoneTitleRepository.findByUserId(userId);
    List<UserZoneTitleResDto> earnedDtos =
        earned.stream().map(UserZoneTitleResDto::from).collect(Collectors.toList());

    List<ZoneProgressResDto> progressList = new ArrayList<>();
    List<String> allZones =
        Arrays.stream(ChatZone.values()).map(Enum::name).collect(Collectors.toList());

    for (String zoneId : allZones) {
      long successCount =
          participationRepository.countByUserIdAndEvent_ZoneIdAndStatus(
              userId, zoneId, ParticipationStatus.SUCCESS);
      List<ZoneTitleDef> defs = titleDefRepository.findByZoneIdOrderByTierAsc(zoneId);

      int currentTier = 0;
      Integer nextRequired = null;
      Integer remaining = null;

      for (ZoneTitleDef def : defs) {
        if (successCount >= def.getRequiredSuccessCount()) {
          currentTier = def.getTier();
        } else if (nextRequired == null) {
          nextRequired = def.getRequiredSuccessCount();
          remaining = (int) Math.max(0, nextRequired - successCount);
        }
      }

      progressList.add(
          new ZoneProgressResDto(zoneId, currentTier, successCount, nextRequired, remaining));
    }

    return new UserTitlesSummaryResDto(equipped, cityGrade, earnedDtos, progressList);
  }
}

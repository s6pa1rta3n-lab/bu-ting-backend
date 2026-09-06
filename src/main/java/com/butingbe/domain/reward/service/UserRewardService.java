package com.butingbe.domain.reward.service;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.chat.entity.ChatZone;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.dto.response.PointLedgerEntryResDto;
import com.butingbe.domain.reward.dto.response.PointLedgerPageResDto;
import com.butingbe.domain.reward.dto.response.UserBadgeItemResDto;
import com.butingbe.domain.reward.dto.response.UserCouponResDto;
import com.butingbe.domain.reward.dto.response.UserRewardsResDto;
import com.butingbe.domain.reward.dto.response.ZoneBadgeGroupResDto;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.entity.UserBadge;
import com.butingbe.domain.reward.entity.UserPointLedger;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.reward.repository.UserBadgeRepository;
import com.butingbe.domain.reward.repository.UserPointLedgerRepository;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 보상 및 포인트 원장 조회 서비스. */
@Service
@RequiredArgsConstructor
public class UserRewardService {

  private final UserPointService userPointService;
  private final UserPointLedgerRepository userPointLedgerRepository;
  private final UserBadgeRepository userBadgeRepository;
  private final RewardGrantRepository rewardGrantRepository;
  private final ZoneEventRepository zoneEventRepository;
  private final FileStorageService fileStorageService;

  /**
   * 유저의 보유 포인트 잔액, 6대 권역 배지 월, 보유 쿠폰 목록을 조회한다.
   *
   * @param user 인증된 유저
   * @return 유저 보유 보상 요약 응답
   */
  @Transactional(readOnly = true)
  public UserRewardsResDto getMyRewards(AuthenticatedUser user) {
    UUID userId = requireUserId(user);

    long pointBalance = userPointService.getBalance(userId);

    List<UserBadge> userBadges = userBadgeRepository.findByUserIdOrderByEarnedAtDesc(userId);
    Set<UUID> grantIds = userBadges.stream().map(UserBadge::getGrantId).collect(Collectors.toSet());
    Map<UUID, RewardGrant> grantMap =
        grantIds.isEmpty()
            ? Map.of()
            : rewardGrantRepository.findAllById(grantIds).stream()
                .collect(Collectors.toMap(RewardGrant::getId, g -> g));

    Set<UUID> eventIds =
        grantMap.values().stream()
            .map(RewardGrant::getEventId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<UUID, ZoneEvent> eventMap =
        eventIds.isEmpty()
            ? Map.of()
            : zoneEventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(ZoneEvent::getId, e -> e));

    Map<String, List<UserBadgeItemResDto>> zoneBadgesMap = new HashMap<>();
    for (ChatZone cz : ChatZone.values()) {
      zoneBadgesMap.put(cz.name(), new ArrayList<>());
    }

    for (UserBadge badge : userBadges) {
      String zoneId = resolveZoneForBadge(badge, grantMap, eventMap);
      String imageUrl =
          badge.getReward().getImageFileKey() != null
              ? fileStorageService.getPresignedUrl(badge.getReward().getImageFileKey())
              : null;
      UserBadgeItemResDto item =
          new UserBadgeItemResDto(
              badge.getId().toString(),
              badge.getReward().getCode(),
              badge.getReward().getName(),
              imageUrl,
              badge.getEarnedAt());

      zoneBadgesMap.computeIfAbsent(zoneId, k -> new ArrayList<>()).add(item);
    }

    List<ZoneBadgeGroupResDto> badgeWall =
        Arrays.stream(ChatZone.values())
            .map(
                cz ->
                    new ZoneBadgeGroupResDto(
                        cz.name(),
                        cz.getZoneName(),
                        zoneBadgesMap.getOrDefault(cz.name(), List.of())))
            .toList();

    List<RewardGrant> userGrants = rewardGrantRepository.findByUserIdOrderByGrantedAtDesc(userId);
    List<UserCouponResDto> coupons =
        userGrants.stream()
            .filter(
                g ->
                    g.getReward().getRewardType() == RewardType.COUPON
                        || g.getReward().getRewardType() == RewardType.GIFTICON)
            .map(
                g -> {
                  OffsetDateTime validUntil =
                      (g.getGrantedAt() != null && g.getReward().getValidDays() != null)
                          ? g.getGrantedAt().plusDays(g.getReward().getValidDays())
                          : null;
                  String cleanId = g.getId().toString().replace("-", "").toUpperCase();
                  String couponCode = "CPN-" + cleanId.substring(0, Math.min(10, cleanId.length()));
                  boolean used = g.getRevokedAt() != null;
                  return new UserCouponResDto(
                      g.getId().toString(),
                      g.getReward().getCode(),
                      g.getReward().getName(),
                      couponCode,
                      validUntil,
                      g.getGrantedAt(),
                      used);
                })
            .toList();

    return new UserRewardsResDto(pointBalance, badgeWall, coupons);
  }

  /**
   * 유저의 포인트 원장 변동 내역을 커서 기반 페이징으로 조회한다.
   *
   * @param user 인증된 유저
   * @param cursor 커서 문자열
   * @param size 페이지 크기
   * @return 포인트 원장 커서 페이징 응답
   */
  @Transactional(readOnly = true)
  public PointLedgerPageResDto getPointLedger(AuthenticatedUser user, String cursor, int size) {
    UUID userId = requireUserId(user);
    int pageSize = size <= 0 ? 20 : Math.min(size, 100);

    CursorDecoded decodedCursor = decodeCursor(cursor);

    Specification<UserPointLedger> spec =
        (root, query, builder) -> {
          var predicate = builder.equal(root.get("userId"), userId);
          if (decodedCursor != null) {
            var timeLess = builder.lessThan(root.get("createdAt"), decodedCursor.createdAt());
            var timeEqual = builder.equal(root.get("createdAt"), decodedCursor.createdAt());
            var idLess = builder.lessThan(root.get("id"), decodedCursor.id());
            predicate =
                builder.and(predicate, builder.or(timeLess, builder.and(timeEqual, idLess)));
          }
          return predicate;
        };

    PageRequest pageRequest =
        PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    List<UserPointLedger> fetched =
        userPointLedgerRepository.findAll(spec, pageRequest).getContent();

    boolean hasNext = fetched.size() > pageSize;
    List<UserPointLedger> items = hasNext ? fetched.subList(0, pageSize) : fetched;

    String nextCursor = null;
    if (hasNext && !items.isEmpty()) {
      UserPointLedger last = items.get(items.size() - 1);
      nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
    }

    List<PointLedgerEntryResDto> resItems =
        items.stream()
            .map(
                entry ->
                    new PointLedgerEntryResDto(
                        entry.getId().toString(),
                        entry.getAmount(),
                        entry.getReason(),
                        entry.getGrantId() != null ? entry.getGrantId().toString() : null,
                        entry.getCreatedAt()))
            .toList();

    return new PointLedgerPageResDto(resItems, nextCursor, hasNext);
  }

  private String resolveZoneForBadge(
      UserBadge badge, Map<UUID, RewardGrant> grantMap, Map<UUID, ZoneEvent> eventMap) {
    RewardGrant grant = grantMap.get(badge.getGrantId());
    if (grant != null && grant.getEventId() != null) {
      ZoneEvent event = eventMap.get(grant.getEventId());
      if (event != null && event.getZoneId() != null) {
        return event.getZoneId();
      }
    }
    return ChatZone.HAEUNDAE_GIJANG.name();
  }

  private String encodeCursor(OffsetDateTime createdAt, UUID id) {
    String raw = createdAt.toString() + "_" + id.toString();
    return Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  private CursorDecoded decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      int idx = raw.lastIndexOf('_');
      if (idx <= 0) {
        throw new IllegalArgumentException("error.common.invalid_cursor");
      }
      OffsetDateTime createdAt = OffsetDateTime.parse(raw.substring(0, idx));
      UUID id = UUID.fromString(raw.substring(idx + 1));
      return new CursorDecoded(createdAt, id);
    } catch (Exception e) {
      throw new IllegalArgumentException("error.common.invalid_cursor", e);
    }
  }

  private record CursorDecoded(OffsetDateTime createdAt, UUID id) {}

  private UUID requireUserId(AuthenticatedUser user) {
    if (user == null || user.id() == null) {
      throw new UnauthenticatedException();
    }
    return user.id();
  }
}

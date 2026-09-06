package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.chat.entity.ChatZone;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.dto.response.GrantedRewardDto;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.zoneevent.dto.response.ParticipationResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventParticipationPageResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.exception.OpenParticipationExistsException;
import com.butingbe.domain.zoneevent.exception.ZoneEventOutOfRangeException;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuthTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.support.GpsDistance;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이벤트 참여 시작, 취소 및 참여 이력 조회 서비스. */
@Service
@RequiredArgsConstructor
public class ZoneEventParticipationService {

  private static final List<ParticipationStatus> OPEN_STATUSES =
      List.of(
          ParticipationStatus.JOINED,
          ParticipationStatus.SUBMITTED,
          ParticipationStatus.UNDER_REVIEW);

  private final ZoneEventRepository zoneEventRepository;
  private final ZoneEventAuthTargetRepository authTargetRepository;
  private final ZoneEventParticipationRepository participationRepository;
  private final RewardGrantRepository rewardGrantRepository;
  private final FileStorageService fileStorageService;

  /** 반경 검증을 통과하면 JOINED 참여를 만들어 돌려준다. */
  @Transactional
  public ParticipationResDto join(
      AuthenticatedUser user, UUID eventId, double latitude, double longitude) {
    UUID userId = requireUserId(user);
    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));
    if (event.getStatus() != ZoneEventStatus.ACTIVE) {
      throw new ConflictException("error.zone_event.not_active");
    }

    ZoneEventAuthTarget target =
        authTargetRepository
            .findByEvent_Id(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));
    int distance =
        GpsDistance.meters(latitude, longitude, target.getLatitude(), target.getLongitude());
    if (distance > target.getRadiusM()) {
      throw new ZoneEventOutOfRangeException(distance);
    }

    participationRepository
        .findByEvent_IdAndUserIdAndStatusIn(eventId, userId, OPEN_STATUSES)
        .ifPresent(
            open -> {
              throw new OpenParticipationExistsException(open.getId());
            });

    long successes =
        participationRepository.countByEvent_IdAndUserIdAndStatus(
            eventId, userId, ParticipationStatus.SUCCESS);
    if (successes >= event.getSuccessLimitPerUser()) {
      throw new ConflictException("error.zone_event.participation.limit_reached");
    }

    ZoneEventParticipation saved;
    try {
      saved =
          participationRepository.save(
              ZoneEventParticipation.join(event, userId, latitude, longitude));
    } catch (DataIntegrityViolationException concurrent) {
      UUID existing =
          participationRepository
              .findByEvent_IdAndUserIdAndStatusIn(eventId, userId, OPEN_STATUSES)
              .map(ZoneEventParticipation::getId)
              .orElse(null);
      throw new OpenParticipationExistsException(existing);
    }

    return ParticipationResDto.of(saved, distance);
  }

  /**
   * 진행 중인 이벤트 참여를 취소한다.
   *
   * @param user 인증된 유저
   * @param eventId 이벤트 식별자
   * @param participationId 참여 식별자
   */
  @Transactional
  public void cancel(AuthenticatedUser user, UUID eventId, UUID participationId) {
    UUID userId = requireUserId(user);
    ZoneEventParticipation participation =
        participationRepository
            .findByIdAndEvent_Id(participationId, eventId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    if (!participation.getUserId().equals(userId)) {
      throw new ForbiddenException("error.zone_event.participation.forbidden");
    }

    if (!participation.getStatus().isOpen()) {
      throw new ConflictException("error.zone_event.participation.invalid_state");
    }

    participation.cancel("USER_CANCELLED");
  }

  /**
   * 특정 이벤트에 대한 유저의 참여 목록을 최신순으로 조회한다.
   *
   * @param user 인증된 유저
   * @param eventId 이벤트 식별자
   * @return 참여 목록
   */
  @Transactional(readOnly = true)
  public List<ParticipationResDto> getMyParticipationsForEvent(
      AuthenticatedUser user, UUID eventId) {
    UUID userId = requireUserId(user);
    List<ZoneEventParticipation> participations =
        participationRepository.findByEvent_IdAndUserIdOrderByJoinedAtDesc(eventId, userId);

    List<UUID> pIds = participations.stream().map(ZoneEventParticipation::getId).toList();
    Map<UUID, List<RewardGrant>> grantsMap =
        pIds.isEmpty()
            ? Map.of()
            : rewardGrantRepository.findByParticipationIdIn(pIds).stream()
                .collect(Collectors.groupingBy(RewardGrant::getParticipationId));

    return participations.stream()
        .map(
            p -> {
              String mediaUrl =
                  p.getMediaFileKey() != null
                      ? fileStorageService.getPresignedUrl(p.getMediaFileKey())
                      : null;
              List<GrantedRewardDto> rewards =
                  grantsMap.getOrDefault(p.getId(), List.of()).stream()
                      .map(GrantedRewardDto::of)
                      .toList();
              return ParticipationResDto.of(p, null, mediaUrl, rewards);
            })
        .toList();
  }

  /**
   * 유저의 전체 구역 이벤트 참여 내역을 커서 기반 페이징으로 조회한다.
   *
   * @param user 인증된 유저
   * @param cursor 커서 문자열
   * @param size 페이지 크기
   * @param zone 구역 필터
   * @param type 이벤트 유형 필터
   * @param status 참여 상태 필터
   * @param from 시작 시각 필터
   * @param to 종료 시각 필터
   * @return 커서 페이징 참여 내역
   */
  @Transactional(readOnly = true)
  public ZoneEventParticipationPageResDto getMyParticipations(
      AuthenticatedUser user,
      String cursor,
      int size,
      String zone,
      String type,
      ParticipationStatus status,
      OffsetDateTime from,
      OffsetDateTime to) {
    UUID userId = requireUserId(user);
    int pageSize = size <= 0 ? 10 : Math.min(size, 50);

    String resolvedZone = null;
    if (zone != null && !zone.isBlank()) {
      resolvedZone = ChatZone.fromString(zone).name();
    }

    CursorDecoded decodedCursor = decodeCursor(cursor);

    Specification<ZoneEventParticipation> spec =
        buildParticipationSpec(userId, resolvedZone, type, status, from, to, decodedCursor);

    PageRequest pageRequest =
        PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "joinedAt", "id"));
    List<ZoneEventParticipation> fetched =
        participationRepository.findAll(spec, pageRequest).getContent();

    boolean hasNext = fetched.size() > pageSize;
    List<ZoneEventParticipation> items = hasNext ? fetched.subList(0, pageSize) : fetched;

    String nextCursor = null;
    if (hasNext && !items.isEmpty()) {
      ZoneEventParticipation last = items.get(items.size() - 1);
      nextCursor = encodeCursor(last.getJoinedAt(), last.getId());
    }

    List<UUID> pIds = items.stream().map(ZoneEventParticipation::getId).toList();
    Map<UUID, List<RewardGrant>> grantsMap =
        pIds.isEmpty()
            ? Map.of()
            : rewardGrantRepository.findByParticipationIdIn(pIds).stream()
                .collect(Collectors.groupingBy(RewardGrant::getParticipationId));

    List<ParticipationResDto> resItems =
        items.stream()
            .map(
                p -> {
                  String mediaUrl =
                      p.getMediaFileKey() != null
                          ? fileStorageService.getPresignedUrl(p.getMediaFileKey())
                          : null;
                  List<GrantedRewardDto> rewards =
                      grantsMap.getOrDefault(p.getId(), List.of()).stream()
                          .map(GrantedRewardDto::of)
                          .toList();
                  return ParticipationResDto.of(p, null, mediaUrl, rewards);
                })
            .toList();

    return new ZoneEventParticipationPageResDto(resItems, nextCursor, hasNext);
  }

  private Specification<ZoneEventParticipation> buildParticipationSpec(
      UUID userId,
      String zoneId,
      String typeCode,
      ParticipationStatus status,
      OffsetDateTime from,
      OffsetDateTime to,
      CursorDecoded cursor) {
    return (root, query, builder) -> {
      var predicate = builder.equal(root.get("userId"), userId);

      if (zoneId != null) {
        predicate = builder.and(predicate, builder.equal(root.get("event").get("zoneId"), zoneId));
      }
      if (typeCode != null && !typeCode.isBlank()) {
        predicate =
            builder.and(
                predicate, builder.equal(root.get("event").get("type").get("typeCode"), typeCode));
      }
      if (status != null) {
        predicate = builder.and(predicate, builder.equal(root.get("status"), status));
      }
      if (from != null) {
        predicate =
            builder.and(predicate, builder.greaterThanOrEqualTo(root.get("joinedAt"), from));
      }
      if (to != null) {
        predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("joinedAt"), to));
      }
      if (cursor != null) {
        var cursorTimeLess = builder.lessThan(root.get("joinedAt"), cursor.joinedAt());
        var cursorTimeEqual = builder.equal(root.get("joinedAt"), cursor.joinedAt());
        var cursorIdLess = builder.lessThan(root.get("id"), cursor.id());
        predicate =
            builder.and(
                predicate, builder.or(cursorTimeLess, builder.and(cursorTimeEqual, cursorIdLess)));
      }

      return predicate;
    };
  }

  private String encodeCursor(OffsetDateTime joinedAt, UUID id) {
    String raw = joinedAt.toString() + "_" + id.toString();
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
      OffsetDateTime joinedAt = OffsetDateTime.parse(raw.substring(0, idx));
      UUID id = UUID.fromString(raw.substring(idx + 1));
      return new CursorDecoded(joinedAt, id);
    } catch (Exception e) {
      throw new IllegalArgumentException("error.common.invalid_cursor", e);
    }
  }

  private record CursorDecoded(OffsetDateTime joinedAt, UUID id) {}

  private UUID requireUserId(AuthenticatedUser user) {
    if (user == null || user.id() == null) {
      throw new UnauthenticatedException();
    }
    return user.id();
  }
}

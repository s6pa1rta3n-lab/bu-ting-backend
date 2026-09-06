package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.chat.entity.ChatZone;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.repository.RewardCatalogRepository;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventTargetReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventUpdateReqDto;
import com.butingbe.domain.zoneevent.dto.request.RewardSnapshotReqDto;
import com.butingbe.domain.zoneevent.dto.response.AdminZoneEventPageResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventDetailResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventSummaryResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventType;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuthTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventTypeRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 구역 이벤트 운영 및 라이프사이클 관리 서비스. */
@Service
@RequiredArgsConstructor
public class AdminZoneEventService {

  private static final List<ParticipationStatus> OPEN_STATUSES =
      List.of(
          ParticipationStatus.JOINED,
          ParticipationStatus.SUBMITTED,
          ParticipationStatus.UNDER_REVIEW);

  private final ZoneEventRepository zoneEventRepository;
  private final ZoneEventTypeRepository zoneEventTypeRepository;
  private final ZoneEventAuthTargetRepository authTargetRepository;
  private final ZoneEventParticipationRepository participationRepository;
  private final RewardCatalogRepository rewardCatalogRepository;
  private final FileStorageService fileStorageService;

  /**
   * 신규 구역 이벤트 및 인증 타겟을 생성한다.
   *
   * @param user 인증된 관리자 유저
   * @param req 생성 요청 데이터
   * @return 생성된 이벤트 상세 응답
   */
  @Transactional
  public ZoneEventDetailResDto createEvent(AuthenticatedUser user, AdminZoneEventCreateReqDto req) {
    verifyOperator(user);

    ChatZone chatZone = ChatZone.fromString(req.zoneId());

    ZoneEventType eventType =
        zoneEventTypeRepository
            .findById(req.typeCode())
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event_type.not_found"));

    AdminZoneEventTargetReqDto targetReq = req.target();
    if (targetReq.radiusM() < 30 || targetReq.radiusM() > 500) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    validateRewardSnapshot(req.baseReward());
    validateRewardSnapshot(req.excellenceReward());

    RewardSnapshot baseReward = req.baseReward().toSnapshot();
    RewardSnapshot excellenceReward =
        req.excellenceReward() != null ? req.excellenceReward().toSnapshot() : null;

    ZoneEvent event =
        ZoneEvent.builder()
            .zoneId(chatZone.name())
            .type(eventType)
            .title(req.title())
            .description(req.description())
            .startsAt(req.startsAt())
            .durationMinutes(req.durationMinutes())
            .status(ZoneEventStatus.SCHEDULED)
            .baseReward(baseReward)
            .excellenceReward(excellenceReward)
            .successLimitPerUser(req.successLimitPerUser() == null ? 1 : req.successLimitPerUser())
            .build();

    ZoneEvent savedEvent = zoneEventRepository.save(event);

    ZoneEventAuthTarget target =
        ZoneEventAuthTarget.builder()
            .event(savedEvent)
            .targetKind(targetReq.targetKind())
            .landmarkId(targetReq.landmarkId())
            .placeName(targetReq.placeName())
            .guideText(targetReq.guideText())
            .exampleFileKey(targetReq.exampleFileKey())
            .latitude(targetReq.latitude())
            .longitude(targetReq.longitude())
            .radiusM(targetReq.radiusM())
            .build();

    ZoneEventAuthTarget savedTarget = authTargetRepository.save(target);

    return toDetailDto(savedEvent, savedTarget, 0L);
  }

  /**
   * 관리자 구역 이벤트 목록을 조건 검색 및 페이징 조회한다.
   *
   * @param user 인증된 관리자 유저
   * @param zone 구역 필터
   * @param status 이벤트 상태 필터
   * @param from 시작 일시 필터
   * @param to 종료 일시 필터
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 이벤트 요약 목록 페이징 응답
   */
  @Transactional(readOnly = true)
  public AdminZoneEventPageResDto getEvents(
      AuthenticatedUser user,
      String zone,
      ZoneEventStatus status,
      OffsetDateTime from,
      OffsetDateTime to,
      int page,
      int size) {
    verifyOperator(user);

    int pageNum = Math.max(page, 0);
    int pageSize = size <= 0 ? 20 : Math.min(size, 100);

    String resolvedZone = null;
    if (zone != null && !zone.isBlank()) {
      resolvedZone = ChatZone.fromString(zone).name();
    }

    Specification<ZoneEvent> spec = buildEventSpec(resolvedZone, status, from, to);
    PageRequest pageRequest =
        PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "startsAt", "id"));
    Page<ZoneEvent> eventPage = zoneEventRepository.findAll(spec, pageRequest);

    List<ZoneEventSummaryResDto> items =
        eventPage.getContent().stream()
            .map(
                event -> {
                  ZoneEventAuthTarget target =
                      authTargetRepository.findByEvent_Id(event.getId()).orElse(null);
                  long successCount =
                      participationRepository.countByEvent_IdAndStatus(
                          event.getId(), ParticipationStatus.SUCCESS);
                  long remainingSeconds = calculateRemainingSeconds(event);
                  return ZoneEventSummaryResDto.of(
                      event, target, remainingSeconds, successCount, null, null);
                })
            .toList();

    return new AdminZoneEventPageResDto(
        items,
        eventPage.getNumber(),
        eventPage.getSize(),
        eventPage.getTotalElements(),
        eventPage.getTotalPages());
  }

  /**
   * 관리자 구역 이벤트 단건 상세 정보를 조회한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @return 이벤트 상세 정보
   */
  @Transactional(readOnly = true)
  public ZoneEventDetailResDto getEventDetail(AuthenticatedUser user, UUID eventId) {
    verifyOperator(user);

    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    ZoneEventAuthTarget target =
        authTargetRepository
            .findByEvent_Id(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    long successCount =
        participationRepository.countByEvent_IdAndStatus(eventId, ParticipationStatus.SUCCESS);

    return toDetailDto(event, target, successCount);
  }

  /**
   * 구역 이벤트 정보를 수정한다. 상태(SCHEDULED/ACTIVE)에 따라 허용 필드를 검증한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @param req 수정 요청 데이터
   * @return 수정된 이벤트 상세 정보
   */
  @Transactional
  public ZoneEventDetailResDto updateEvent(
      AuthenticatedUser user, UUID eventId, AdminZoneEventUpdateReqDto req) {
    verifyOperator(user);

    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    ZoneEventAuthTarget target =
        authTargetRepository
            .findByEvent_Id(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    if (event.getStatus() == ZoneEventStatus.CLOSED
        || event.getStatus() == ZoneEventStatus.CANCELLED) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    if (event.getStatus() == ZoneEventStatus.ACTIVE) {
      if (req.zoneId() != null && !req.zoneId().equalsIgnoreCase(event.getZoneId())) {
        throw new ConflictException("error.zone_event.invalid_state");
      }
      if (req.typeCode() != null && !req.typeCode().equals(event.getType().getTypeCode())) {
        throw new ConflictException("error.zone_event.invalid_state");
      }
      if (req.startsAt() != null && !req.startsAt().isEqual(event.getStartsAt())) {
        throw new ConflictException("error.zone_event.invalid_state");
      }
      if (req.baseReward() != null) {
        throw new ConflictException("error.zone_event.invalid_state");
      }
      if (req.target() != null) {
        throw new ConflictException("error.zone_event.invalid_state");
      }

      validateRewardSnapshot(req.excellenceReward());
      RewardSnapshot excellenceReward =
          req.excellenceReward() != null ? req.excellenceReward().toSnapshot() : null;

      event.updateActive(
          req.title(),
          req.description(),
          req.durationMinutes(),
          excellenceReward,
          req.successLimitPerUser());
    } else {
      String zoneId = req.zoneId() != null ? ChatZone.fromString(req.zoneId()).name() : null;
      ZoneEventType eventType = null;
      if (req.typeCode() != null) {
        eventType =
            zoneEventTypeRepository
                .findById(req.typeCode())
                .orElseThrow(
                    () -> new ResourceNotFoundException("error.zone_event_type.not_found"));
      }

      validateRewardSnapshot(req.baseReward());
      validateRewardSnapshot(req.excellenceReward());

      RewardSnapshot baseReward = req.baseReward() != null ? req.baseReward().toSnapshot() : null;
      RewardSnapshot excellenceReward =
          req.excellenceReward() != null ? req.excellenceReward().toSnapshot() : null;

      event.updateScheduled(
          zoneId,
          eventType,
          req.title(),
          req.description(),
          req.startsAt(),
          req.durationMinutes(),
          baseReward,
          excellenceReward,
          req.successLimitPerUser());

      if (req.target() != null) {
        AdminZoneEventTargetReqDto tReq = req.target();
        if (tReq.radiusM() != null && (tReq.radiusM() < 30 || tReq.radiusM() > 500)) {
          throw new ConflictException("error.zone_event.invalid_state");
        }
        target.update(
            tReq.targetKind(),
            tReq.landmarkId(),
            tReq.placeName(),
            tReq.guideText(),
            tReq.exampleFileKey(),
            tReq.latitude(),
            tReq.longitude(),
            tReq.radiusM());
      }
    }

    long successCount =
        participationRepository.countByEvent_IdAndStatus(eventId, ParticipationStatus.SUCCESS);

    return toDetailDto(event, target, successCount);
  }

  /**
   * SCHEDULED 상태의 이벤트를 ACTIVE 상태로 전환한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @return 상태 전환된 이벤트 상세 정보
   */
  @Transactional
  public ZoneEventDetailResDto activateEvent(AuthenticatedUser user, UUID eventId) {
    verifyOperator(user);

    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    if (event.getStatus() != ZoneEventStatus.SCHEDULED) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    event.activate();

    ZoneEventAuthTarget target =
        authTargetRepository
            .findByEvent_Id(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    long successCount =
        participationRepository.countByEvent_IdAndStatus(eventId, ParticipationStatus.SUCCESS);

    return toDetailDto(event, target, successCount);
  }

  /**
   * ACTIVE 상태의 이벤트를 CLOSED 상태로 전환한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @return 상태 전환된 이벤트 상세 정보
   */
  @Transactional
  public ZoneEventDetailResDto closeEvent(AuthenticatedUser user, UUID eventId) {
    verifyOperator(user);

    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    if (event.getStatus() != ZoneEventStatus.ACTIVE) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    event.close();

    ZoneEventAuthTarget target =
        authTargetRepository
            .findByEvent_Id(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    long successCount =
        participationRepository.countByEvent_IdAndStatus(eventId, ParticipationStatus.SUCCESS);

    return toDetailDto(event, target, successCount);
  }

  /**
   * 이벤트를 취소하고 진행 중이던 모든 참여를 취소 처리한다.
   *
   * @param user 인증된 관리자 유저
   * @param eventId 이벤트 식별자
   * @return 취소 처리된 이벤트 상세 정보
   */
  @Transactional
  public ZoneEventDetailResDto cancelEvent(AuthenticatedUser user, UUID eventId) {
    verifyOperator(user);

    ZoneEvent event =
        zoneEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    if (event.getStatus() != ZoneEventStatus.SCHEDULED
        && event.getStatus() != ZoneEventStatus.ACTIVE) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    event.cancel();

    List<ZoneEventParticipation> openParticipations =
        participationRepository.findByEvent_IdAndStatusIn(eventId, OPEN_STATUSES);
    for (ZoneEventParticipation p : openParticipations) {
      p.cancel("EVENT_CANCELLED");
    }

    ZoneEventAuthTarget target =
        authTargetRepository
            .findByEvent_Id(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.not_found"));

    long successCount =
        participationRepository.countByEvent_IdAndStatus(eventId, ParticipationStatus.SUCCESS);

    return toDetailDto(event, target, successCount);
  }

  private void validateRewardSnapshot(RewardSnapshotReqDto dto) {
    if (dto == null) {
      return;
    }
    if (dto.badge() != null && !dto.badge().isBlank()) {
      if (!rewardCatalogRepository.existsByCode(dto.badge())) {
        throw new ResourceNotFoundException("error.reward.invalid_code");
      }
    }
    if (dto.catalogCode() != null && !dto.catalogCode().isBlank()) {
      if (!rewardCatalogRepository.existsByCode(dto.catalogCode())) {
        throw new ResourceNotFoundException("error.reward.invalid_code");
      }
    }
  }

  private ZoneEventDetailResDto toDetailDto(
      ZoneEvent event, ZoneEventAuthTarget target, long successCount) {
    long remainingSeconds = calculateRemainingSeconds(event);
    String exampleImageUrl =
        target.getExampleFileKey() != null
            ? fileStorageService.getPresignedUrl(target.getExampleFileKey())
            : null;
    return ZoneEventDetailResDto.of(
        event, target, exampleImageUrl, remainingSeconds, successCount, null);
  }

  private long calculateRemainingSeconds(ZoneEvent event) {
    OffsetDateTime now = OffsetDateTime.now();
    if (now.isBefore(event.getStartsAt())) {
      return Duration.between(now, event.getStartsAt()).toSeconds();
    }
    OffsetDateTime endsAt = event.endsAt();
    if (now.isBefore(endsAt)) {
      return Duration.between(now, endsAt).toSeconds();
    }
    return 0L;
  }

  private Specification<ZoneEvent> buildEventSpec(
      String zoneId, ZoneEventStatus status, OffsetDateTime from, OffsetDateTime to) {
    return (root, query, builder) -> {
      var predicate = builder.conjunction();
      if (zoneId != null) {
        predicate = builder.and(predicate, builder.equal(root.get("zoneId"), zoneId));
      }
      if (status != null) {
        predicate = builder.and(predicate, builder.equal(root.get("status"), status));
      }
      if (from != null) {
        predicate =
            builder.and(predicate, builder.greaterThanOrEqualTo(root.get("startsAt"), from));
      }
      if (to != null) {
        predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("startsAt"), to));
      }
      return predicate;
    };
  }

  private void verifyOperator(AuthenticatedUser user) {
    if (user == null || (user.id() == null && !user.isDevelopmentAdmin())) {
      throw new UnauthenticatedException();
    }
    boolean isOp =
        user.isDevelopmentAdmin()
            || (user.authorities() != null
                && user.authorities().stream()
                    .anyMatch(
                        a ->
                            "ROLE_ADMIN".equals(a.getAuthority())
                                || "ROLE_MANAGER".equals(a.getAuthority())));
    if (!isOp) {
      throw new ForbiddenException("error.operator.forbidden");
    }
  }
}

package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.zoneevent.dto.response.ParticipationResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind;
import com.butingbe.domain.zoneevent.entity.ZoneEventType;
import com.butingbe.domain.zoneevent.exception.OpenParticipationExistsException;
import com.butingbe.domain.zoneevent.exception.ZoneEventOutOfRangeException;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuthTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ZoneEventParticipationServiceTest {

  private static final UUID EVENT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("22222222-0000-0000-0000-000000000001");
  private static final UUID OPEN_ID = UUID.fromString("33333333-0000-0000-0000-000000000001");
  private static final double IN_LAT = 35.1532;
  private static final double IN_LNG = 129.1182;
  private static final double OUT_LAT = 35.16;
  private static final double OUT_LNG = 129.13;

  @Mock private ZoneEventRepository zoneEventRepository;
  @Mock private ZoneEventAuthTargetRepository authTargetRepository;
  @Mock private ZoneEventParticipationRepository participationRepository;
  @Mock private com.butingbe.domain.reward.repository.RewardGrantRepository rewardGrantRepository;
  @Mock private com.butingbe.domain.file.service.FileStorageService fileStorageService;

  private ZoneEventParticipationService service;
  private AuthenticatedUser user;
  private ZoneEvent event;

  @BeforeEach
  void setUp() {
    service =
        new ZoneEventParticipationService(
            zoneEventRepository,
            authTargetRepository,
            participationRepository,
            rewardGrantRepository,
            fileStorageService);
    user = new AuthenticatedUser(USER_ID, "u@example.com", "u", List.of());
    event = event(ZoneEventStatus.ACTIVE, 1);
  }

  @Test
  @DisplayName("반경 이내면 JOINED 참여를 만들고 거리를 채운다")
  void joinWithinRadius() {
    stubActiveEventWithTarget();
    when(participationRepository.findByEvent_IdAndUserIdAndStatusIn(
            eq(EVENT_ID), eq(USER_ID), any()))
        .thenReturn(Optional.empty());
    when(participationRepository.countByEvent_IdAndUserIdAndStatus(
            EVENT_ID, USER_ID, ParticipationStatus.SUCCESS))
        .thenReturn(0L);
    when(participationRepository.save(any()))
        .thenAnswer(
            invocation -> {
              ZoneEventParticipation p = invocation.getArgument(0);
              ReflectionTestUtils.setField(p, "id", OPEN_ID);
              return p;
            });

    ParticipationResDto result = service.join(user, EVENT_ID, IN_LAT, IN_LNG);

    assertThat(result.status()).isEqualTo("JOINED");
    assertThat(result.distanceM()).isEqualTo(28);
    assertThat(result.zoneId()).isEqualTo("SUYEONG_NAMGU");
    assertThat(result.visibility()).isEqualTo("PUBLIC");
  }

  @Test
  @DisplayName("반경 밖이면 400(out_of_range)이고 거리를 담는다")
  void joinOutOfRange() {
    stubActiveEventWithTarget();

    assertThatThrownBy(() -> service.join(user, EVENT_ID, OUT_LAT, OUT_LNG))
        .isInstanceOf(ZoneEventOutOfRangeException.class)
        .satisfies(
            e ->
                assertThat(((ZoneEventOutOfRangeException) e).getDistanceMeters()).isEqualTo(1340));
  }

  @Test
  @DisplayName("이미 열린 참여가 있으면 409(already_open)에 기존 id를 담는다")
  void joinWhenOpenExists() {
    stubActiveEventWithTarget();
    ZoneEventParticipation open = ZoneEventParticipation.join(event, USER_ID, IN_LAT, IN_LNG);
    ReflectionTestUtils.setField(open, "id", OPEN_ID);
    when(participationRepository.findByEvent_IdAndUserIdAndStatusIn(
            eq(EVENT_ID), eq(USER_ID), any()))
        .thenReturn(Optional.of(open));

    assertThatThrownBy(() -> service.join(user, EVENT_ID, IN_LAT, IN_LNG))
        .isInstanceOf(OpenParticipationExistsException.class)
        .satisfies(
            e ->
                assertThat(((OpenParticipationExistsException) e).getParticipationId())
                    .isEqualTo(OPEN_ID));
  }

  @Test
  @DisplayName("성공 상한에 도달하면 409(limit_reached)다")
  void joinWhenLimitReached() {
    stubActiveEventWithTarget();
    when(participationRepository.findByEvent_IdAndUserIdAndStatusIn(
            eq(EVENT_ID), eq(USER_ID), any()))
        .thenReturn(Optional.empty());
    when(participationRepository.countByEvent_IdAndUserIdAndStatus(
            EVENT_ID, USER_ID, ParticipationStatus.SUCCESS))
        .thenReturn(1L);

    assertThatThrownBy(() -> service.join(user, EVENT_ID, IN_LAT, IN_LNG))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.zone_event.participation.limit_reached");
  }

  @Test
  @DisplayName("동시 요청으로 부분 UK를 위반하면 409(already_open)로 매핑한다")
  void joinConcurrentConflict() {
    stubActiveEventWithTarget();
    ZoneEventParticipation open = ZoneEventParticipation.join(event, USER_ID, IN_LAT, IN_LNG);
    ReflectionTestUtils.setField(open, "id", OPEN_ID);
    when(participationRepository.findByEvent_IdAndUserIdAndStatusIn(
            eq(EVENT_ID), eq(USER_ID), any()))
        .thenReturn(Optional.empty(), Optional.of(open));
    when(participationRepository.countByEvent_IdAndUserIdAndStatus(any(), any(), any()))
        .thenReturn(0L);
    when(participationRepository.save(any()))
        .thenThrow(new DataIntegrityViolationException("uk_zone_event_participation_open"));

    assertThatThrownBy(() -> service.join(user, EVENT_ID, IN_LAT, IN_LNG))
        .isInstanceOf(OpenParticipationExistsException.class)
        .satisfies(
            e ->
                assertThat(((OpenParticipationExistsException) e).getParticipationId())
                    .isEqualTo(OPEN_ID));
  }

  @Test
  @DisplayName("ACTIVE가 아니면 409(not_active)다")
  void joinWhenNotActive() {
    when(zoneEventRepository.findById(EVENT_ID))
        .thenReturn(Optional.of(event(ZoneEventStatus.SCHEDULED, 1)));

    assertThatThrownBy(() -> service.join(user, EVENT_ID, IN_LAT, IN_LNG))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.zone_event.not_active");
  }

  @Test
  @DisplayName("없는 이벤트는 404다")
  void joinWhenEventNotFound() {
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.join(user, EVENT_ID, IN_LAT, IN_LNG))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("error.zone_event.not_found");
  }

  @Test
  @DisplayName("타겟이 없는 이벤트는 404다")
  void joinWhenTargetMissing() {
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.join(user, EVENT_ID, IN_LAT, IN_LNG))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("미인증이면 401이다")
  void joinWhenUnauthenticated() {
    assertThatThrownBy(() -> service.join(null, EVENT_ID, IN_LAT, IN_LNG))
        .isInstanceOf(UnauthenticatedException.class);
  }

  private void stubActiveEventWithTarget() {
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    lenient()
        .when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(target(event)));
  }

  private ZoneEvent event(ZoneEventStatus status, int successLimit) {
    ZoneEventType type =
        ZoneEventType.builder().typeCode("PLACE_AUTH").name("장소 인증").requiresUpload(true).build();
    ZoneEvent created =
        ZoneEvent.builder()
            .zoneId("SUYEONG_NAMGU")
            .type(type)
            .title("광안대교 야경 담기")
            .startsAt(OffsetDateTime.now().minusHours(1))
            .durationMinutes(1440)
            .status(status)
            .baseReward(new RewardSnapshot(50, "SPOT_GWANGAN_BRIDGE", null, null))
            .successLimitPerUser(successLimit)
            .build();
    ReflectionTestUtils.setField(created, "id", EVENT_ID);
    return created;
  }

  private ZoneEventAuthTarget target(ZoneEvent event) {
    return ZoneEventAuthTarget.builder()
        .event(event)
        .targetKind(ZoneEventTargetKind.PLACE)
        .placeName("광안대교 야경")
        .latitude(35.153)
        .longitude(129.118)
        .radiusM(100)
        .build();
  }

  @Test
  @DisplayName("참여자가 본인의 진행 중 참여를 취소할 수 있다")
  void cancelSuccess() {
    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(USER_ID)
            .gpsLat(35.153)
            .gpsLng(129.118)
            .joinedAt(OffsetDateTime.now())
            .status(ParticipationStatus.JOINED)
            .build();
    ReflectionTestUtils.setField(p, "id", OPEN_ID);

    when(participationRepository.findByIdAndEvent_Id(OPEN_ID, EVENT_ID)).thenReturn(Optional.of(p));

    service.cancel(user, EVENT_ID, OPEN_ID);

    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.CANCELLED);
    assertThat(p.getCancelReason()).isEqualTo("USER_CANCELLED");
  }

  @Test
  @DisplayName("타인의 참여를 취소하려고 하면 403 Forbidden이다")
  void cancelWhenNotOwner() {
    UUID otherUser = UUID.randomUUID();
    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(otherUser)
            .gpsLat(35.153)
            .gpsLng(129.118)
            .joinedAt(OffsetDateTime.now())
            .status(ParticipationStatus.JOINED)
            .build();
    ReflectionTestUtils.setField(p, "id", OPEN_ID);

    when(participationRepository.findByIdAndEvent_Id(OPEN_ID, EVENT_ID)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.cancel(user, EVENT_ID, OPEN_ID))
        .isInstanceOf(com.butingbe.global.error.exception.ForbiddenException.class);
  }

  @Test
  @DisplayName("이미 성공/실패 처리된 참여는 취소할 수 없다 (409 Conflict)")
  void cancelWhenAlreadyEnded() {
    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(USER_ID)
            .gpsLat(35.153)
            .gpsLng(129.118)
            .joinedAt(OffsetDateTime.now())
            .status(ParticipationStatus.SUCCESS)
            .build();
    ReflectionTestUtils.setField(p, "id", OPEN_ID);

    when(participationRepository.findByIdAndEvent_Id(OPEN_ID, EVENT_ID)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.cancel(user, EVENT_ID, OPEN_ID))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("존재하지 않는 참여 취소 시 404이다")
  void cancelWhenNotFound() {
    when(participationRepository.findByIdAndEvent_Id(OPEN_ID, EVENT_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.cancel(user, EVENT_ID, OPEN_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("특정 이벤트에 대한 내 참여 이력을 조회할 수 있다")
  void getMyParticipationsForEvent() {
    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(USER_ID)
            .gpsLat(35.153)
            .gpsLng(129.118)
            .joinedAt(OffsetDateTime.now())
            .status(ParticipationStatus.SUCCESS)
            .build();
    ReflectionTestUtils.setField(p, "id", OPEN_ID);
    ReflectionTestUtils.setField(p, "mediaFileKey", "media/sample.jpg");

    when(participationRepository.findByEvent_IdAndUserIdOrderByJoinedAtDesc(EVENT_ID, USER_ID))
        .thenReturn(List.of(p));
    when(rewardGrantRepository.findByParticipationIdIn(any())).thenReturn(List.of());
    when(fileStorageService.getPresignedUrl("media/sample.jpg"))
        .thenReturn("https://s3.example.com/media.jpg");

    List<ParticipationResDto> result = service.getMyParticipationsForEvent(user, EVENT_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).participationId()).isEqualTo(OPEN_ID.toString());
    assertThat(result.get(0).mediaUrl()).isEqualTo("https://s3.example.com/media.jpg");
  }

  @Test
  @DisplayName("내 전체 구역 이벤트 참여 이력을 커서 페이징 조회할 수 있다")
  void getMyParticipations() {
    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(USER_ID)
            .gpsLat(35.153)
            .gpsLng(129.118)
            .joinedAt(OffsetDateTime.now())
            .status(ParticipationStatus.SUCCESS)
            .build();
    ReflectionTestUtils.setField(p, "id", OPEN_ID);

    org.springframework.data.domain.Page<ZoneEventParticipation> page =
        new org.springframework.data.domain.PageImpl<>(List.of(p));
    when(participationRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            any(org.springframework.data.domain.PageRequest.class)))
        .thenReturn(page);
    when(rewardGrantRepository.findByParticipationIdIn(any())).thenReturn(List.of());

    var response = service.getMyParticipations(user, null, 10, null, null, null, null, null);

    assertThat(response.items()).hasSize(1);
    assertThat(response.hasNext()).isFalse();
  }

  @Test
  @DisplayName("참여 이력 커서 디코딩 및 필터 조건을 적용하여 조회한다")
  void getMyParticipationsWithCursorAndFilters() {
    ZoneEventParticipation p1 =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(USER_ID)
            .gpsLat(35.153)
            .gpsLng(129.118)
            .joinedAt(OffsetDateTime.now())
            .status(ParticipationStatus.SUCCESS)
            .build();
    ReflectionTestUtils.setField(p1, "id", OPEN_ID);
    ReflectionTestUtils.setField(p1, "mediaFileKey", "media/sample.jpg");

    when(fileStorageService.getPresignedUrl("media/sample.jpg"))
        .thenReturn("https://s3.example.com/media.jpg");

    ZoneEventParticipation p2 =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(USER_ID)
            .gpsLat(35.153)
            .gpsLng(129.118)
            .joinedAt(OffsetDateTime.now().minusMinutes(10))
            .status(ParticipationStatus.SUCCESS)
            .build();
    ReflectionTestUtils.setField(p2, "id", UUID.randomUUID());

    org.springframework.data.domain.Page<ZoneEventParticipation> page =
        new org.springframework.data.domain.PageImpl<>(List.of(p1, p2));
    when(participationRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            any(org.springframework.data.domain.PageRequest.class)))
        .thenAnswer(
            inv -> {
              org.springframework.data.jpa.domain.Specification<ZoneEventParticipation> spec =
                  inv.getArgument(0);
              if (spec != null) {
                jakarta.persistence.criteria.Root<ZoneEventParticipation> root =
                    mock(
                        jakarta.persistence.criteria.Root.class,
                        org.mockito.Mockito.RETURNS_DEEP_STUBS);
                jakarta.persistence.criteria.CriteriaQuery<?> cq =
                    mock(jakarta.persistence.criteria.CriteriaQuery.class);
                jakarta.persistence.criteria.CriteriaBuilder cb =
                    mock(
                        jakarta.persistence.criteria.CriteriaBuilder.class,
                        org.mockito.Mockito.RETURNS_DEEP_STUBS);
                spec.toPredicate(root, cq, cb);
              }
              return page;
            });
    when(rewardGrantRepository.findByParticipationIdIn(any())).thenReturn(List.of());

    String cursor =
        java.util.Base64.getUrlEncoder()
            .encodeToString("2026-09-01T00:00:00Z_00000000-0000-0000-0000-000000000001".getBytes());

    var response =
        service.getMyParticipations(
            user,
            cursor,
            1,
            "SUYEONG_NAMGU",
            "PLACE_AUTH",
            ParticipationStatus.SUCCESS,
            OffsetDateTime.now().minusDays(1),
            OffsetDateTime.now().plusDays(1));

    assertThat(response.items()).hasSize(1);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isNotNull();
    assertThat(response.items().get(0).mediaUrl()).isEqualTo("https://s3.example.com/media.jpg");
  }

  @Test
  @DisplayName("참여 이력이 없는 경우 빈 페이지를 반환한다")
  void getMyParticipationsEmpty() {
    when(participationRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            any(org.springframework.data.domain.PageRequest.class)))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

    var response = service.getMyParticipations(user, null, 10, null, null, null, null, null);
    assertThat(response.items()).isEmpty();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
  }

  @Test
  @DisplayName("잘못된 커서 전달 시 IllegalArgumentException을 던진다")
  void getMyParticipationsInvalidCursor() {
    assertThatThrownBy(
            () ->
                service.getMyParticipations(
                    user, "invalid_cursor", 10, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("존재하지 않는 참여 취소 시 404를 반환한다")
  void cancelNotFound() {
    when(participationRepository.findByIdAndEvent_Id(OPEN_ID, EVENT_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.cancel(user, EVENT_ID, OPEN_ID))
        .isInstanceOf(com.butingbe.global.error.exception.ResourceNotFoundException.class);
  }
}

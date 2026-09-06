package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.repository.RewardCatalogRepository;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventTargetReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventUpdateReqDto;
import com.butingbe.domain.zoneevent.dto.request.RewardSnapshotReqDto;
import com.butingbe.domain.zoneevent.dto.response.AdminZoneEventPageResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventDetailResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind;
import com.butingbe.domain.zoneevent.entity.ZoneEventType;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuthTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventTypeRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminZoneEventServiceTest {

  private static final UUID ADMIN_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();

  @Mock private ZoneEventRepository zoneEventRepository;
  @Mock private ZoneEventTypeRepository zoneEventTypeRepository;
  @Mock private ZoneEventAuthTargetRepository authTargetRepository;
  @Mock private ZoneEventParticipationRepository participationRepository;
  @Mock private RewardCatalogRepository rewardCatalogRepository;
  @Mock private FileStorageService fileStorageService;

  private AdminZoneEventService service;
  private AuthenticatedUser adminUser;
  private AuthenticatedUser normalUser;
  private ZoneEventType sampleType;

  @BeforeEach
  void setUp() {
    service =
        new AdminZoneEventService(
            zoneEventRepository,
            zoneEventTypeRepository,
            authTargetRepository,
            participationRepository,
            rewardCatalogRepository,
            fileStorageService);

    adminUser =
        new AuthenticatedUser(
            ADMIN_ID,
            "admin@example.com",
            "admin",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    normalUser =
        new AuthenticatedUser(
            UUID.randomUUID(),
            "user@example.com",
            "user",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    sampleType =
        ZoneEventType.builder().typeCode("PLACE_AUTH").name("장소 인증").requiresUpload(true).build();
  }

  @Test
  @DisplayName("비관리자 유저는 이벤트 생성 시 403 Forbidden이다")
  void createEventForbidden() {
    AdminZoneEventCreateReqDto req = createReq(100);
    assertThatThrownBy(() -> service.createEvent(normalUser, req))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("반경이 30m 미만이거나 500m 초과인 경우 409 Conflict이다")
  void createEventInvalidRadius() {
    AdminZoneEventCreateReqDto reqTooSmall = createReq(20);
    when(zoneEventTypeRepository.findById("PLACE_AUTH")).thenReturn(Optional.of(sampleType));

    assertThatThrownBy(() -> service.createEvent(adminUser, reqTooSmall))
        .isInstanceOf(ConflictException.class);

    AdminZoneEventCreateReqDto reqTooLarge = createReq(600);
    assertThatThrownBy(() -> service.createEvent(adminUser, reqTooLarge))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("정상적인 파라미터로 이벤트를 생성하면 SCHEDULED 상태로 저장된다")
  void createEventSuccess() {
    AdminZoneEventCreateReqDto req = createReq(100);
    when(zoneEventTypeRepository.findById("PLACE_AUTH")).thenReturn(Optional.of(sampleType));
    when(rewardCatalogRepository.existsByCode("SPOT_GWANGAN_BRIDGE")).thenReturn(true);

    when(zoneEventRepository.save(any()))
        .thenAnswer(
            inv -> {
              ZoneEvent e = inv.getArgument(0);
              ReflectionTestUtils.setField(e, "id", EVENT_ID);
              return e;
            });

    when(authTargetRepository.save(any()))
        .thenAnswer(
            inv -> {
              ZoneEventAuthTarget t = inv.getArgument(0);
              ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
              return t;
            });

    ZoneEventDetailResDto result = service.createEvent(adminUser, req);

    assertThat(result.eventId()).isEqualTo(EVENT_ID.toString());
    assertThat(result.status()).isEqualTo("SCHEDULED");
  }

  @Test
  @DisplayName("SCHEDULED 이벤트를 활성화(ACTIVE)할 수 있다")
  void activateEventSuccess() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.SCHEDULED);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    ZoneEventDetailResDto res = service.activateEvent(adminUser, EVENT_ID);

    assertThat(res.status()).isEqualTo("ACTIVE");
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.ACTIVE);
  }

  @Test
  @DisplayName("ACTIVE 상태가 아닌 이벤트를 활성화하려고 하면 409 Conflict이다")
  void activateEventConflict() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.ACTIVE);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

    assertThatThrownBy(() -> service.activateEvent(adminUser, EVENT_ID))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("ACTIVE 이벤트를 종료(CLOSED)할 수 있다")
  void closeEventSuccess() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.ACTIVE);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    ZoneEventDetailResDto res = service.closeEvent(adminUser, EVENT_ID);

    assertThat(res.status()).isEqualTo("CLOSED");
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.CLOSED);
  }

  @Test
  @DisplayName("이벤트를 취소(CANCELLED)하면 진행 중이던 참여들이 함께 취소된다")
  void cancelEventSuccess() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.ACTIVE);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(UUID.randomUUID())
            .status(ParticipationStatus.JOINED)
            .gpsLat(35.15)
            .gpsLng(129.11)
            .joinedAt(OffsetDateTime.now())
            .build();
    when(participationRepository.findByEvent_IdAndStatusIn(any(), any())).thenReturn(List.of(p));

    ZoneEventDetailResDto res = service.cancelEvent(adminUser, EVENT_ID);

    assertThat(res.status()).isEqualTo("CANCELLED");
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.CANCELLED);
    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.CANCELLED);
    assertThat(p.getCancelReason()).isEqualTo("EVENT_CANCELLED");
  }

  @Test
  @DisplayName("ACTIVE 상태의 이벤트에서 불변 필드를 수정하려고 하면 409 Conflict이다")
  void updateActiveEventImmutableFieldConflict() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.ACTIVE);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    AdminZoneEventUpdateReqDto req =
        new AdminZoneEventUpdateReqDto(
            "HAEUNDAE_GIJANG", null, null, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> service.updateEvent(adminUser, EVENT_ID, req))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("관리자 이벤트 상세를 정상 조회한다")
  void getEventSuccess() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.ACTIVE);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    ZoneEventDetailResDto detail = service.getEventDetail(adminUser, EVENT_ID);
    assertThat(detail.eventId()).isEqualTo(EVENT_ID.toString());
    assertThat(detail.title()).isEqualTo("광안리 행사");
  }

  @Test
  @DisplayName("존재하지 않는 이벤트 상세 조회 시 404를 반환한다")
  void getEventNotFound() {
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getEventDetail(adminUser, EVENT_ID))
        .isInstanceOf(com.butingbe.global.error.exception.ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("SCHEDULED 상태의 이벤트를 전체 수정한다")
  void updateScheduledEventSuccess() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.SCHEDULED);
    ZoneEventAuthTarget target = sampleTarget(event);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID)).thenReturn(Optional.of(target));
    when(zoneEventTypeRepository.findById("PLACE_AUTH")).thenReturn(Optional.of(sampleType));

    com.butingbe.domain.reward.entity.RewardCatalog pointCatalog =
        com.butingbe.domain.reward.entity.RewardCatalog.builder()
            .rewardType(com.butingbe.domain.reward.entity.RewardType.POINT)
            .code("SPOT_GWANGAN_BRIDGE")
            .name("광안리 포인트")
            .active(true)
            .build();
    when(rewardCatalogRepository.existsByCode("SPOT_GWANGAN_BRIDGE")).thenReturn(true);

    AdminZoneEventTargetReqDto targetReq =
        new AdminZoneEventTargetReqDto(
            ZoneEventTargetKind.PLACE, "gwangan-new", "광안리 새위치", "가이드", null, 35.15, 129.11, 150);

    AdminZoneEventUpdateReqDto req =
        new AdminZoneEventUpdateReqDto(
            "HAEUNDAE_GIJANG",
            "PLACE_AUTH",
            "새 타이틀",
            "새 설명",
            OffsetDateTime.now().plusDays(2),
            180,
            targetReq,
            new RewardSnapshotReqDto(50, "SPOT_GWANGAN_BRIDGE", null, null),
            null,
            5);

    ZoneEventDetailResDto res = service.updateEvent(adminUser, EVENT_ID, req);
    assertThat(res.title()).isEqualTo("새 타이틀");
    assertThat(res.zone().zoneId()).isEqualTo("HAEUNDAE_GIJANG");
    assertThat(res.durationMinutes()).isEqualTo(180);
    assertThat(res.successLimitPerUser()).isEqualTo(5);
    assertThat(target.getRadiusM()).isEqualTo(150);
  }

  @Test
  @DisplayName("ACTIVE 상태의 이벤트에서 허용된 필드를 수정한다")
  void updateActiveEventAllowedFieldsSuccess() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.ACTIVE);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    AdminZoneEventUpdateReqDto req =
        new AdminZoneEventUpdateReqDto(
            null, null, "수정된 타이틀", "수정된 설명", null, 240, null, null, null, 3);

    ZoneEventDetailResDto res = service.updateEvent(adminUser, EVENT_ID, req);
    assertThat(res.title()).isEqualTo("수정된 타이틀");
    assertThat(res.description()).isEqualTo("수정된 설명");
    assertThat(res.durationMinutes()).isEqualTo(240);
    assertThat(res.successLimitPerUser()).isEqualTo(3);
  }

  @Test
  @DisplayName("이벤트 수정 시 반경이 유효 범위를 벗어나면 예외가 발생한다")
  void updateEventInvalidRadius() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.SCHEDULED);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    AdminZoneEventTargetReqDto targetReq =
        new AdminZoneEventTargetReqDto(
            ZoneEventTargetKind.PLACE, "gwangan", "광안리", null, null, 35.15, 129.11, 20);

    AdminZoneEventUpdateReqDto req =
        new AdminZoneEventUpdateReqDto(
            null, null, null, null, null, null, targetReq, null, null, null);

    assertThatThrownBy(() -> service.updateEvent(adminUser, EVENT_ID, req))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("CLOSED 이벤트 수정 시 409 Conflict이다")
  void updateClosedEventConflict() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.CLOSED);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    AdminZoneEventUpdateReqDto req =
        new AdminZoneEventUpdateReqDto(null, null, "수정", null, null, null, null, null, null, null);

    assertThatThrownBy(() -> service.updateEvent(adminUser, EVENT_ID, req))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("ACTIVE 상태가 아닌 이벤트를 닫으려고 하면 409 Conflict이다")
  void closeEventNonActiveConflict() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.SCHEDULED);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

    assertThatThrownBy(() -> service.closeEvent(adminUser, EVENT_ID))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("이미 종료된 이벤트를 취소하려고 하면 409 Conflict이다")
  void cancelEventClosedConflict() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.CLOSED);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

    assertThatThrownBy(() -> service.cancelEvent(adminUser, EVENT_ID))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("관리자 이벤트 목록을 다양한 조건으로 조회한다")
  void getEventsFilters() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.ACTIVE);
    when(zoneEventRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenAnswer(
            inv -> {
              Specification<ZoneEvent> spec = inv.getArgument(0);
              if (spec != null) {
                jakarta.persistence.criteria.Root<ZoneEvent> root =
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
              return new PageImpl<>(List.of(event));
            });
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    OffsetDateTime from = OffsetDateTime.now().minusDays(1);
    OffsetDateTime to = OffsetDateTime.now().plusDays(1);

    AdminZoneEventPageResDto res =
        service.getEvents(adminUser, "SUYEONG_NAMGU", ZoneEventStatus.ACTIVE, from, to, 0, 20);

    assertThat(res.items()).hasSize(1);
    assertThat(res.totalElements()).isEqualTo(1L);
  }

  @Test
  @DisplayName("ACTIVE 이벤트의 구역, 유형, 시작시간, 기본보상, 대상 수정 시 409 Conflict이다")
  void updateActiveEventForbiddenFieldsConflict() {
    ZoneEvent event = sampleEvent(ZoneEventStatus.ACTIVE);
    when(zoneEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    when(authTargetRepository.findByEvent_Id(EVENT_ID))
        .thenReturn(Optional.of(sampleTarget(event)));

    AdminZoneEventUpdateReqDto diffZone =
        new AdminZoneEventUpdateReqDto(
            "HAEUNDAE_GIJANG", null, null, null, null, null, null, null, null, null);
    assertThatThrownBy(() -> service.updateEvent(adminUser, EVENT_ID, diffZone))
        .isInstanceOf(ConflictException.class);

    AdminZoneEventUpdateReqDto diffType =
        new AdminZoneEventUpdateReqDto(
            null, "DIFF_TYPE", null, null, null, null, null, null, null, null);
    assertThatThrownBy(() -> service.updateEvent(adminUser, EVENT_ID, diffType))
        .isInstanceOf(ConflictException.class);

    AdminZoneEventUpdateReqDto diffStart =
        new AdminZoneEventUpdateReqDto(
            null, null, null, null, OffsetDateTime.now().plusDays(5), null, null, null, null, null);
    assertThatThrownBy(() -> service.updateEvent(adminUser, EVENT_ID, diffStart))
        .isInstanceOf(ConflictException.class);

    AdminZoneEventUpdateReqDto diffBase =
        new AdminZoneEventUpdateReqDto(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new RewardSnapshotReqDto(100, null, null, null),
            null,
            null);
    assertThatThrownBy(() -> service.updateEvent(adminUser, EVENT_ID, diffBase))
        .isInstanceOf(ConflictException.class);

    AdminZoneEventUpdateReqDto diffTarget =
        new AdminZoneEventUpdateReqDto(
            null,
            null,
            null,
            null,
            null,
            null,
            new AdminZoneEventTargetReqDto(
                ZoneEventTargetKind.PLACE, "x", "x", null, null, 35.1, 129.1, 100),
            null,
            null,
            null);
    assertThatThrownBy(() -> service.updateEvent(adminUser, EVENT_ID, diffTarget))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("보상 스냅샷의 카탈로그 코드가 존재하지 않으면 404이다")
  void validateRewardSnapshotCatalogNotFound() {
    AdminZoneEventCreateReqDto req = createReq(100);
    when(zoneEventTypeRepository.findById("PLACE_AUTH")).thenReturn(Optional.of(sampleType));
    when(rewardCatalogRepository.existsByCode("SPOT_GWANGAN_BRIDGE")).thenReturn(false);

    assertThatThrownBy(() -> service.createEvent(adminUser, req))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("관리자 권한이 없으면 403 Forbidden이다")
  void verifyOperatorForbidden() {
    AuthenticatedUser normalUser =
        new AuthenticatedUser(
            UUID.randomUUID(),
            "user@example.com",
            "user",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    assertThatThrownBy(() -> service.getEvents(normalUser, null, null, null, null, 0, 10))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 401 Unauthenticated이다")
  void verifyOperatorUnauthenticated() {
    assertThatThrownBy(() -> service.getEvents(null, null, null, null, null, 0, 10))
        .isInstanceOf(UnauthenticatedException.class);
  }

  private AdminZoneEventCreateReqDto createReq(int radiusM) {
    AdminZoneEventTargetReqDto target =
        new AdminZoneEventTargetReqDto(
            ZoneEventTargetKind.PLACE, "gwangan", "광안대교", "인증 가이드", null, 35.153, 129.118, radiusM);
    RewardSnapshotReqDto base = new RewardSnapshotReqDto(50, "SPOT_GWANGAN_BRIDGE", null, null);

    return new AdminZoneEventCreateReqDto(
        "SUYEONG_NAMGU",
        "PLACE_AUTH",
        "광안리 행사",
        "설명",
        OffsetDateTime.now().plusHours(1),
        120,
        target,
        base,
        null,
        1);
  }

  private ZoneEvent sampleEvent(ZoneEventStatus status) {
    ZoneEvent e =
        ZoneEvent.builder()
            .zoneId("SUYEONG_NAMGU")
            .type(sampleType)
            .title("광안리 행사")
            .startsAt(OffsetDateTime.now())
            .durationMinutes(120)
            .status(status)
            .baseReward(new RewardSnapshot(50, "SPOT_GWANGAN_BRIDGE", null, null))
            .successLimitPerUser(1)
            .build();
    ReflectionTestUtils.setField(e, "id", EVENT_ID);
    return e;
  }

  private ZoneEventAuthTarget sampleTarget(ZoneEvent event) {
    ZoneEventAuthTarget t =
        ZoneEventAuthTarget.builder()
            .event(event)
            .targetKind(ZoneEventTargetKind.PLACE)
            .placeName("광안대교")
            .latitude(35.153)
            .longitude(129.118)
            .radiusM(100)
            .build();
    ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
    return t;
  }
}

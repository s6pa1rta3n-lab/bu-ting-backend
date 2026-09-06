package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.notification.service.NotificationService;
import com.butingbe.domain.reward.service.RewardRevokeService;
import com.butingbe.domain.reward.service.RewardService;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.response.ReviewQueueItemResDto;
import com.butingbe.domain.zoneevent.dto.response.ReviewQueuePageResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuditLog;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuditLogRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuthTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.global.error.exception.ConflictException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminZoneEventReviewServiceTest {

  private static final UUID REVIEWER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");
  private static final UUID EVENT_ID = UUID.fromString("33333333-0000-0000-0000-000000000003");
  private static final UUID PARTICIPATION_ID =
      UUID.fromString("44444444-0000-0000-0000-000000000004");

  @Mock private ZoneEventParticipationRepository participationRepository;
  @Mock private ZoneEventReportRepository reportRepository;
  @Mock private ZoneEventAuditLogRepository auditLogRepository;
  @Mock private ZoneEventAuthTargetRepository authTargetRepository;
  @Mock private UserRepository userRepository;
  @Mock private RewardService rewardService;
  @Mock private RewardRevokeService rewardRevokeService;
  @Mock private ZoneTitleService zoneTitleService;
  @Mock private NotificationService notificationService;
  @Mock private FileStorageService fileStorageService;

  @InjectMocks private AdminZoneEventReviewService reviewService;

  private ZoneEventParticipation makeParticipation(ParticipationStatus status, boolean hidden) {
    ZoneEvent event =
        ZoneEvent.builder()
            .title("이벤트")
            .zoneId("GWANGAN")
            .baseReward(new RewardSnapshot(50, "BADGE_GWANGAN", null, null))
            .build();
    ReflectionTestUtils.setField(event, "id", EVENT_ID);

    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(USER_ID)
            .status(status)
            .gpsLat(35.0)
            .gpsLng(129.0)
            .joinedAt(java.time.OffsetDateTime.now())
            .build();
    ReflectionTestUtils.setField(p, "id", PARTICIPATION_ID);
    ReflectionTestUtils.setField(p, "hidden", hidden);
    ReflectionTestUtils.setField(p, "submitGpsLat", 35.153);
    ReflectionTestUtils.setField(p, "submitGpsLng", 129.118);
    ReflectionTestUtils.setField(p, "capturedAt", java.time.OffsetDateTime.now().minusMinutes(20));
    ReflectionTestUtils.setField(p, "completedAt", java.time.OffsetDateTime.now());
    ReflectionTestUtils.setField(p, "mediaFileKey", "media.jpg");
    ReflectionTestUtils.setField(p, "content", "인증내용");
    return p;
  }

  @Test
  @DisplayName("getReviewQueue maps queue items correctly")
  void getReviewQueue_success() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.UNDER_REVIEW, true);
    when(participationRepository.findByStatusOrHiddenTrue(any(), any()))
        .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    ZoneEventAuthTarget target =
        ZoneEventAuthTarget.builder()
            .latitude(35.153)
            .longitude(129.118)
            .radiusM(50)
            .placeName("장소")
            .build();
    when(authTargetRepository.findByEvent_Id(EVENT_ID)).thenReturn(Optional.of(target));
    when(reportRepository.countByParticipationIdAndStatus(PARTICIPATION_ID, ReportStatus.OPEN))
        .thenReturn(2L);
    when(fileStorageService.getPresignedUrl("media.jpg")).thenReturn("http://presigned.jpg");

    ReviewQueuePageResDto pageResult = reviewService.getReviewQueue(0, 10);
    assertThat(pageResult.items()).hasSize(1);
    ReviewQueueItemResDto item = pageResult.items().get(0);
    assertThat(item.flaggedReasons()).contains("HIDDEN_BY_REPORTS");
    assertThat(item.distanceM()).isNotNull();
  }

  @Test
  @DisplayName("approve transitions under-review participation to success and grants rewards")
  void approve_success() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.UNDER_REVIEW, true);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    ZoneEventReport report =
        ZoneEventReport.builder().participation(p).status(ReportStatus.OPEN).build();
    when(reportRepository.findByParticipationIdAndStatus(PARTICIPATION_ID, ReportStatus.OPEN))
        .thenReturn(List.of(report));

    reviewService.approve(REVIEWER_ID, PARTICIPATION_ID);

    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.SUCCESS);
    assertThat(p.getHidden()).isFalse();
    assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    verify(rewardService).grantBaseReward(USER_ID, PARTICIPATION_ID, EVENT_ID, 50, "BADGE_GWANGAN");
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
    verify(notificationService).sendToUser(eq(USER_ID), any(), any(), eq("ZONE_EVENT"));
  }

  @Test
  @DisplayName("approve throws ConflictException when status is not UNDER_REVIEW")
  void approve_conflict() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.SUCCESS, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> reviewService.approve(REVIEWER_ID, PARTICIPATION_ID))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("reject transitions under-review participation to rejected")
  void reject_success() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.UNDER_REVIEW, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    reviewService.reject(REVIEWER_ID, PARTICIPATION_ID, "GPS 불일치");

    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.FAIL);
    assertThat(p.getFailReason()).isEqualTo("GPS 불일치");
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
    verify(notificationService).sendToUser(eq(USER_ID), any(), any(), eq("ZONE_EVENT"));
  }

  @Test
  @DisplayName("reject throws ConflictException when status is not UNDER_REVIEW")
  void reject_conflict() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.CANCELLED, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> reviewService.reject(REVIEWER_ID, PARTICIPATION_ID, "사유"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("revoke rolls back rewards for success participation")
  void revoke_success() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.SUCCESS, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    reviewService.revoke(REVIEWER_ID, PARTICIPATION_ID, "부정행위 발견");

    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.REVOKED);
    verify(rewardRevokeService).revokeParticipation(PARTICIPATION_ID);
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
    verify(notificationService).sendToUser(eq(USER_ID), any(), any(), eq("ZONE_EVENT"));
  }

  @Test
  @DisplayName("revoke throws ConflictException when status is not SUCCESS")
  void revoke_conflict() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.UNDER_REVIEW, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> reviewService.revoke(REVIEWER_ID, PARTICIPATION_ID, "사유"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("unhide unhides participation and dismisses open reports")
  void unhide_success() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.SUCCESS, true);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    ZoneEventReport report =
        ZoneEventReport.builder().participation(p).status(ReportStatus.OPEN).build();
    when(reportRepository.findByParticipationIdAndStatus(PARTICIPATION_ID, ReportStatus.OPEN))
        .thenReturn(List.of(report));

    reviewService.unhide(REVIEWER_ID, PARTICIPATION_ID);

    assertThat(p.getHidden()).isFalse();
    assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
    verify(auditLogRepository).save(any(ZoneEventAuditLog.class));
  }

  @Test
  @DisplayName("NotFound exceptions for review operations")
  void reviewOperations_notFound() {
    UUID nonExistent = UUID.randomUUID();
    when(participationRepository.findById(nonExistent)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.approve(REVIEWER_ID, nonExistent))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> reviewService.reject(REVIEWER_ID, nonExistent, "사유"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> reviewService.revoke(REVIEWER_ID, nonExistent, "사유"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> reviewService.unhide(REVIEWER_ID, nonExistent))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getReviewQueue covers capture time delta and unknown user")
  void getReviewQueue_extraBranches() {
    ZoneEventParticipation p = makeParticipation(ParticipationStatus.UNDER_REVIEW, false);
    ReflectionTestUtils.setField(p, "capturedAt", OffsetDateTime.now().minusHours(1));
    ReflectionTestUtils.setField(p, "completedAt", OffsetDateTime.now());

    Page<ZoneEventParticipation> page = new PageImpl<>(List.of(p));
    when(participationRepository.findByStatusOrHiddenTrue(
            eq(ParticipationStatus.UNDER_REVIEW), any(Pageable.class)))
        .thenReturn(page);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
    when(authTargetRepository.findByEvent_Id(EVENT_ID)).thenReturn(Optional.empty());
    when(reportRepository.countByParticipationIdAndStatus(PARTICIPATION_ID, ReportStatus.OPEN))
        .thenReturn(1L);

    ReviewQueuePageResDto res = reviewService.getReviewQueue(0, 10);
    assertThat(res.items()).hasSize(1);
    assertThat(res.items().get(0).userNickname()).isEqualTo("Unknown");
    assertThat(res.items().get(0).flaggedReasons()).isNotEmpty();
  }
}

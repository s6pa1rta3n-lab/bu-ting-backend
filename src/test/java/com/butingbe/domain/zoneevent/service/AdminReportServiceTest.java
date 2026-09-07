package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.entity.BaseRewardPayout;
import com.butingbe.domain.reward.entity.PayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.repository.BaseRewardPayoutRepository;
import com.butingbe.domain.reward.repository.RewardPayoutRepository;
import com.butingbe.domain.user.entity.Name;
import com.butingbe.domain.user.entity.User;
import com.butingbe.domain.user.entity.UserRole;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.request.AdminReportDismissReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminReportUpholdReqDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportDetailResDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportPageResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.entity.ReportReasonCode;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventType;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventTypeRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.support.AbstractContainerTest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminReportServiceTest extends AbstractContainerTest {

  @Autowired private AdminReportService adminReportService;
  @Autowired private ZoneEventReportRepository reportRepository;
  @Autowired private ZoneEventParticipationRepository participationRepository;
  @Autowired private ZoneEventRepository zoneEventRepository;
  @Autowired private ZoneEventTypeRepository zoneEventTypeRepository;
  @Autowired private BaseRewardPayoutRepository baseRewardPayoutRepository;
  @Autowired private RewardPayoutRepository rewardPayoutRepository;
  @Autowired private UserRepository userRepository;

  private ZoneEvent event;
  private AuthenticatedUser operator;
  private AuthenticatedUser normalUser;
  private UUID roundId;

  @BeforeEach
  void setUp() {
    roundId = UUID.randomUUID();
    ZoneEventType type =
        zoneEventTypeRepository.save(
            ZoneEventType.builder()
                .typeCode("PLACE_AUTH")
                .name("장소 인증")
                .requiresUpload(true)
                .build());

    event =
        zoneEventRepository.save(
            ZoneEvent.builder()
                .roundId(roundId)
                .zoneId("SUYEONG_NAMGU")
                .type(type)
                .title("테스트 이벤트")
                .startsAt(OffsetDateTime.now().minusHours(1))
                .durationMinutes(1440)
                .status(ZoneEventStatus.ACTIVE)
                .baseReward(new RewardSnapshot(50, null, null, null))
                .successLimitPerUser(1)
                .build());

    User opUser =
        userRepository.save(
            User.builder()
                .email("op-" + UUID.randomUUID() + "@example.com")
                .provider("google")
                .providerId("google-" + UUID.randomUUID())
                .name(new Name("Admin", "Kim"))
                .nickname("admin")
                .role(UserRole.ADMIN)
                .build());
    operator =
        new AuthenticatedUser(
            opUser.getId(),
            opUser.getEmail(),
            opUser.getNickname(),
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    User regularUser =
        userRepository.save(
            User.builder()
                .email("user-" + UUID.randomUUID() + "@example.com")
                .provider("google")
                .providerId("google-" + UUID.randomUUID())
                .name(new Name("User", "Lee"))
                .nickname("user")
                .role(UserRole.USER)
                .build());
    normalUser =
        new AuthenticatedUser(
            regularUser.getId(),
            regularUser.getEmail(),
            regularUser.getNickname(),
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Test
  @DisplayName("신고 목록 조회 및 조건별(status, roundId, eventId, participationId) 필터링")
  void getReportsWithFilters() {
    ZoneEventParticipation p1 = createParticipation();
    ZoneEventParticipation p2 = createParticipation();

    ZoneEventReport r1 =
        reportRepository.save(
            ZoneEventReport.builder()
                .participationId(p1.getId())
                .reporterId(UUID.randomUUID())
                .reasonCode(ReportReasonCode.NOT_ON_SITE)
                .memo("현장 아님")
                .build());

    ZoneEventReport r2 =
        reportRepository.save(
            ZoneEventReport.builder()
                .participationId(p2.getId())
                .reporterId(UUID.randomUUID())
                .reasonCode(ReportReasonCode.SPAM)
                .memo("도배")
                .build());
    r2.resolveAs(ReportStatus.DISMISSED);
    reportRepository.save(r2);

    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(p1.getId())
                .reward(new RewardSnapshot(50, null, null, null))
                .build());
    basePayout.hold();
    baseRewardPayoutRepository.save(basePayout);

    AdminReportPageResDto all =
        adminReportService.getReports(operator, null, null, null, null, PageRequest.of(0, 10));
    assertThat(all.totalElements()).isEqualTo(2);

    AdminReportPageResDto openOnly =
        adminReportService.getReports(
            operator, ReportStatus.OPEN, null, null, null, PageRequest.of(0, 10));
    assertThat(openOnly.totalElements()).isEqualTo(1);
    assertThat(openOnly.items().get(0).reportId()).isEqualTo(r1.getId());
    assertThat(openOnly.items().get(0).holdStatus()).isEqualTo(PayoutHoldStatus.HELD_REPORT);

    AdminReportPageResDto byRound =
        adminReportService.getReports(operator, null, roundId, null, null, PageRequest.of(0, 10));
    assertThat(byRound.totalElements()).isEqualTo(2);

    AdminReportPageResDto byEvent =
        adminReportService.getReports(
            operator, null, null, event.getId(), null, PageRequest.of(0, 10));
    assertThat(byEvent.totalElements()).isEqualTo(2);

    AdminReportPageResDto byPart =
        adminReportService.getReports(
            operator, null, null, null, p2.getId(), PageRequest.of(0, 10));
    assertThat(byPart.totalElements()).isEqualTo(1);
    assertThat(byPart.items().get(0).reportId()).isEqualTo(r2.getId());
  }

  @Test
  @DisplayName("신고 상세 조회 시 신고 정보, 검수 대상 참여 정보 및 관련 지급 건이 함께 반환된다")
  void getReportDetail() {
    ZoneEventParticipation p = createParticipation();
    ZoneEventReport r =
        reportRepository.save(
            ZoneEventReport.builder()
                .participationId(p.getId())
                .reporterId(UUID.randomUUID())
                .reasonCode(ReportReasonCode.NOT_ON_SITE)
                .memo("현장 아님")
                .build());

    baseRewardPayoutRepository.save(
        BaseRewardPayout.builder()
            .participationId(p.getId())
            .reward(new RewardSnapshot(50, null, null, null))
            .build());

    rewardPayoutRepository.save(
        RewardPayout.builder()
            .eventId(event.getId())
            .participationId(p.getId())
            .rankN(1)
            .likeCountAtClose(20L)
            .reward(new RewardSnapshot(100, null, null, null))
            .build());

    AdminReportDetailResDto detail = adminReportService.getReportDetail(operator, r.getId());

    assertThat(detail.report().reportId()).isEqualTo(r.getId());
    assertThat(detail.report().reasonCode()).isEqualTo(ReportReasonCode.NOT_ON_SITE);
    assertThat(detail.target().participationId()).isEqualTo(p.getId());
    assertThat(detail.target().eventId()).isEqualTo(event.getId());
    assertThat(detail.relatedPayouts()).hasSize(2);
  }

  @Test
  @DisplayName("신고 인정(UPHELD) 처리 시 상태가 변경되고 지급이 보류된다")
  void upholdReport() {
    ZoneEventParticipation p = createParticipation();
    ZoneEventReport r =
        reportRepository.save(
            ZoneEventReport.builder()
                .participationId(p.getId())
                .reporterId(UUID.randomUUID())
                .reasonCode(ReportReasonCode.NOT_ON_SITE)
                .memo("현장 아님")
                .build());

    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(p.getId())
                .reward(new RewardSnapshot(50, null, null, null))
                .build());

    AdminReportUpholdReqDto req = new AdminReportUpholdReqDto("부정 참여 인정", "HOLD", r.getRevision());
    AdminReportDetailResDto updated = adminReportService.upholdReport(operator, r.getId(), req);

    assertThat(updated.report().status()).isEqualTo(ReportStatus.UPHELD);
    assertThat(updated.report().reviewedBy()).isEqualTo(operator.id());
    assertThat(updated.report().decisionNote()).isEqualTo("부정 참여 인정");

    BaseRewardPayout reloaded =
        baseRewardPayoutRepository.findById(basePayout.getId()).orElseThrow();
    assertThat(reloaded.getHoldStatus()).isEqualTo(PayoutHoldStatus.HELD_REPORT);
  }

  @Test
  @DisplayName("신고 인정 시 리비전 불일치면 409 예외가 발생한다")
  void upholdReportRevisionConflict() {
    ZoneEventParticipation p = createParticipation();
    ZoneEventReport r =
        reportRepository.save(
            ZoneEventReport.builder()
                .participationId(p.getId())
                .reporterId(UUID.randomUUID())
                .reasonCode(ReportReasonCode.NOT_ON_SITE)
                .memo("현장 아님")
                .build());

    AdminReportUpholdReqDto req = new AdminReportUpholdReqDto("메모", "HOLD", 9999L);
    assertThatThrownBy(() -> adminReportService.upholdReport(operator, r.getId(), req))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.revision.conflict");
  }

  @Test
  @DisplayName("신고 기각(DISMISSED) 처리 시 상태가 변경되며 지급 보류는 자동 해제되지 않는다")
  void dismissReportKeepsHold() {
    ZoneEventParticipation p = createParticipation();
    ZoneEventReport r =
        reportRepository.save(
            ZoneEventReport.builder()
                .participationId(p.getId())
                .reporterId(UUID.randomUUID())
                .reasonCode(ReportReasonCode.NOT_ON_SITE)
                .memo("현장 아님")
                .build());

    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(p.getId())
                .reward(new RewardSnapshot(50, null, null, null))
                .build());
    basePayout.hold();
    baseRewardPayoutRepository.save(basePayout);

    AdminReportDismissReqDto req = new AdminReportDismissReqDto("정상 참여 확인", r.getRevision());
    AdminReportDetailResDto updated = adminReportService.dismissReport(operator, r.getId(), req);

    assertThat(updated.report().status()).isEqualTo(ReportStatus.DISMISSED);
    assertThat(updated.report().decisionNote()).isEqualTo("정상 참여 확인");

    BaseRewardPayout reloaded =
        baseRewardPayoutRepository.findById(basePayout.getId()).orElseThrow();
    assertThat(reloaded.getHoldStatus()).isEqualTo(PayoutHoldStatus.HELD_REPORT);
  }

  @Test
  @DisplayName("일반 사용자가 신고 관리 기능을 호출하면 403 예외가 발생한다")
  void nonOperatorForbidden() {
    assertThatThrownBy(
            () ->
                adminReportService.getReports(
                    normalUser, null, null, null, null, PageRequest.of(0, 10)))
        .isInstanceOf(ForbiddenException.class);
  }

  private ZoneEventParticipation createParticipation() {
    User user =
        userRepository.save(
            User.builder()
                .email("participant-" + UUID.randomUUID() + "@example.com")
                .provider("google")
                .providerId("google-" + UUID.randomUUID())
                .name(new Name("Kim", "User"))
                .nickname("participant")
                .role(UserRole.USER)
                .build());

    return participationRepository.save(
        ZoneEventParticipation.builder()
            .event(event)
            .userId(user.getId())
            .status(ParticipationStatus.SUCCESS)
            .gpsLat(35.1)
            .gpsLng(129.1)
            .joinedAt(OffsetDateTime.now())
            .visibility(ParticipationVisibility.PUBLIC)
            .build());
  }
}

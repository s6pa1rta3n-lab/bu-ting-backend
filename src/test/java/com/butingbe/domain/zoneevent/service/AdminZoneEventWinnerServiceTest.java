package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.entity.PayoutHoldStatus;
import com.butingbe.domain.reward.repository.RewardPayoutRepository;
import com.butingbe.domain.user.entity.Name;
import com.butingbe.domain.user.entity.User;
import com.butingbe.domain.user.entity.UserRole;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.request.ConfirmWinnersReqDto;
import com.butingbe.domain.zoneevent.dto.response.ConfirmWinnersResDto;
import com.butingbe.domain.zoneevent.dto.response.PayoutGenerateResDto;
import com.butingbe.domain.zoneevent.dto.response.RoundTopNResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.entity.ReportReasonCode;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.RoundType;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuditLog;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import com.butingbe.domain.zoneevent.entity.ZoneEventRound;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventType;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuditLogRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRankingSnapshotRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventTypeRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.support.AbstractContainerTest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminZoneEventWinnerServiceTest extends AbstractContainerTest {

  @Autowired private AdminZoneEventWinnerService winnerService;
  @Autowired private ZoneEventRoundRepository roundRepository;
  @Autowired private ZoneEventRepository zoneEventRepository;
  @Autowired private ZoneEventTypeRepository zoneEventTypeRepository;
  @Autowired private ZoneEventParticipationRepository participationRepository;
  @Autowired private ZoneEventRankingSnapshotRepository snapshotRepository;
  @Autowired private ZoneEventReportRepository reportRepository;
  @Autowired private RewardPayoutRepository rewardPayoutRepository;
  @Autowired private ZoneEventAuditLogRepository auditLogRepository;
  @Autowired private UserRepository userRepository;

  private AuthenticatedUser operatorUser;
  private AuthenticatedUser regularUser;
  private ZoneEventRound round;
  private ZoneEvent event;
  private ZoneEventParticipation p1;
  private ZoneEventParticipation p2;
  private ZoneEventParticipation p3;
  private ZoneEventParticipation p4;

  @BeforeEach
  void setUp() {
    User op =
        userRepository.save(
            User.builder()
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .provider("google")
                .providerId("g-" + UUID.randomUUID())
                .name(new Name("Admin", "Kim"))
                .nickname("admin")
                .role(UserRole.ADMIN)
                .build());
    operatorUser =
        new AuthenticatedUser(
            op.getId(),
            op.getEmail(),
            op.getNickname(),
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    User reg =
        userRepository.save(
            User.builder()
                .email("user-" + UUID.randomUUID() + "@example.com")
                .provider("google")
                .providerId("g-" + UUID.randomUUID())
                .name(new Name("Regular", "User"))
                .nickname("regular")
                .role(UserRole.USER)
                .build());
    regularUser =
        new AuthenticatedUser(
            reg.getId(),
            reg.getEmail(),
            reg.getNickname(),
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    ZoneEventType type =
        zoneEventTypeRepository.save(
            ZoneEventType.builder()
                .typeCode("PHOTO_AUTH_" + UUID.randomUUID().toString().substring(0, 5))
                .name("사진 인증")
                .requiresUpload(true)
                .build());

    round =
        roundRepository.save(
            ZoneEventRound.builder()
                .roundNo(10)
                .roundType(RoundType.REGULAR)
                .startsAt(OffsetDateTime.now().minusDays(2))
                .endsAt(OffsetDateTime.now().minusDays(1))
                .timezone("Asia/Seoul")
                .status(RoundStatus.CLOSED)
                .build());

    event =
        zoneEventRepository.save(
            ZoneEvent.builder()
                .zoneId("SUYEONG_NAMGU")
                .type(type)
                .roundId(round.getId())
                .slotCode("1-A")
                .title("광안리 인증")
                .startsAt(round.getStartsAt())
                .durationMinutes(1440)
                .status(ZoneEventStatus.ACTIVE)
                .baseReward(new RewardSnapshot(100, "POINT", null, null))
                .excellenceReward(new RewardSnapshot(null, "COUPON", 2, "COUPON_CHICKEN"))
                .build());

    p1 = createParticipation(op.getId(), 10L);
    p2 = createParticipation(reg.getId(), 5L);
    p3 = createParticipation(UUID.randomUUID(), 5L);
    p4 = createParticipation(UUID.randomUUID(), 1L);
  }

  @Test
  @DisplayName("Top N 조회: 경계 동점자 및 신고 보류 플래그를 정상 계산하고 동결한다")
  void getTopNWithBoundaryTieAndReports() {
    reportRepository.save(
        ZoneEventReport.builder()
            .participationId(p2.getId())
            .reporterId(UUID.randomUUID())
            .reasonCode(ReportReasonCode.NOT_ON_SITE)
            .build());

    RoundTopNResDto result = winnerService.getTopN(operatorUser, round.getId(), event.getId());

    assertThat(result.events()).hasSize(1);
    var eventDto = result.events().get(0);
    assertThat(eventDto.eventId()).isEqualTo(event.getId());
    assertThat(eventDto.hasTiedBoundary()).isTrue();
    assertThat(eventDto.finalized()).isFalse();
    assertThat(eventDto.candidates()).hasSize(3);

    var c1 = eventDto.candidates().get(0);
    assertThat(c1.participationId()).isEqualTo(p1.getId());
    assertThat(c1.rankN()).isEqualTo(1);
    assertThat(c1.tied()).isFalse();
    assertThat(c1.heldByReport()).isFalse();

    var c2 =
        eventDto.candidates().stream()
            .filter(c -> c.participationId().equals(p2.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(c2.rankN()).isEqualTo(2);
    assertThat(c2.tied()).isTrue();
    assertThat(c2.heldByReport()).isTrue();
    assertThat(c2.reportCount()).isEqualTo(1);

    var c3 =
        eventDto.candidates().stream()
            .filter(c -> c.participationId().equals(p3.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(c3.rankN()).isEqualTo(2);
    assertThat(c3.tied()).isTrue();
    assertThat(c3.heldByReport()).isFalse();

    RoundTopNResDto secondCall = winnerService.getTopN(operatorUser, round.getId(), null);
    assertThat(secondCall.events()).hasSize(1);
    assertThat(secondCall.events().get(0).candidates()).hasSize(3);
  }

  @Test
  @DisplayName("Top N 조회: 예외 케이스(권한 부족, 회차 없음, 이벤트 회차 불일치)")
  void getTopNExceptions() {
    assertThatThrownBy(() -> winnerService.getTopN(regularUser, round.getId(), event.getId()))
        .isInstanceOf(ForbiddenException.class);

    UUID nonExistentRound = UUID.randomUUID();
    assertThatThrownBy(() -> winnerService.getTopN(operatorUser, nonExistentRound, null))
        .isInstanceOf(ResourceNotFoundException.class);

    ZoneEvent otherEvent =
        zoneEventRepository.save(
            ZoneEvent.builder()
                .zoneId("HAEUNDAE")
                .type(event.getType())
                .roundId(UUID.randomUUID())
                .title("다른 회차 이벤트")
                .startsAt(OffsetDateTime.now())
                .durationMinutes(60)
                .status(ZoneEventStatus.ACTIVE)
                .baseReward(new RewardSnapshot(50, "POINT", null, null))
                .excellenceReward(new RewardSnapshot(null, "COUPON", 1, "COUPON_COFFEE"))
                .build());

    assertThatThrownBy(() -> winnerService.getTopN(operatorUser, round.getId(), otherEvent.getId()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("경계 동점자 관리자 확정: 선정 사유 감사 로그 기록 및 확정")
  void confirmWinnersSuccess() {
    RoundTopNResDto initial = winnerService.getTopN(operatorUser, round.getId(), event.getId());
    UUID snapshotId = initial.events().get(0).snapshotId();

    ConfirmWinnersReqDto request =
        new ConfirmWinnersReqDto(
            snapshotId, List.of(p2.getId()), "사진 구도 및 상세 설명 우수", event.getRevision());

    ConfirmWinnersResDto res = winnerService.confirmWinners(operatorUser, event.getId(), request);

    assertThat(res.eventId()).isEqualTo(event.getId());
    assertThat(res.finalizedParticipationIds()).containsExactlyInAnyOrder(p1.getId(), p2.getId());
    assertThat(res.selectionReason()).isEqualTo("사진 구도 및 상세 설명 우수");

    List<ZoneEventAuditLog> logs = auditLogRepository.findAll();
    assertThat(logs).isNotEmpty();
    ZoneEventAuditLog audit =
        logs.stream()
            .filter(l -> "CONFIRM_WINNERS".equals(l.getAction()))
            .findFirst()
            .orElseThrow();
    assertThat(audit.getTargetId()).isEqualTo(event.getId());
    assertThat(audit.getDetail()).containsEntry("selectionReason", "사진 구도 및 상세 설명 우수");
  }

  @Test
  @DisplayName("경계 동점자 관리자 확정: 예외 케이스(낙관적 락 revision 불일치, 동점자 선택 수 오류, 잘못된 참여 ID)")
  void confirmWinnersValidationErrors() {
    RoundTopNResDto initial = winnerService.getTopN(operatorUser, round.getId(), event.getId());
    UUID snapshotId = initial.events().get(0).snapshotId();

    ConfirmWinnersReqDto wrongRevision =
        new ConfirmWinnersReqDto(snapshotId, List.of(p2.getId()), "사유", 999L);
    assertThatThrownBy(
            () -> winnerService.confirmWinners(operatorUser, event.getId(), wrongRevision))
        .isInstanceOf(ConflictException.class);

    ConfirmWinnersReqDto wrongCount =
        new ConfirmWinnersReqDto(
            snapshotId, List.of(p2.getId(), p3.getId()), "2명 다 선택(초과)", event.getRevision());
    assertThatThrownBy(() -> winnerService.confirmWinners(operatorUser, event.getId(), wrongCount))
        .isInstanceOf(ConflictException.class);

    ConfirmWinnersReqDto invalidParticipation =
        new ConfirmWinnersReqDto(
            snapshotId, List.of(UUID.randomUUID()), "존재하지 않는 참여자", event.getRevision());
    assertThatThrownBy(
            () -> winnerService.confirmWinners(operatorUser, event.getId(), invalidParticipation))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("보상 지급 후보 생성: 확정된 수상자만 대상으로 멱등하게 Payout 생성 및 신고 보류 반영")
  void generatePayoutsSuccessAndIdempotency() {
    reportRepository.save(
        ZoneEventReport.builder()
            .participationId(p2.getId())
            .reporterId(UUID.randomUUID())
            .reasonCode(ReportReasonCode.NOT_ON_SITE)
            .build());

    RoundTopNResDto initial = winnerService.getTopN(operatorUser, round.getId(), event.getId());
    UUID snapshotId = initial.events().get(0).snapshotId();

    winnerService.confirmWinners(
        operatorUser,
        event.getId(),
        new ConfirmWinnersReqDto(snapshotId, List.of(p2.getId()), "선정 사유", event.getRevision()));

    PayoutGenerateResDto generateRes = winnerService.generatePayouts(operatorUser, event.getId());
    assertThat(generateRes.totalGenerated()).isEqualTo(2);
    assertThat(generateRes.heldCount()).isEqualTo(1);
    assertThat(generateRes.payouts()).hasSize(2);

    var payout2 =
        generateRes.payouts().stream()
            .filter(p -> p.participationId().equals(p2.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(payout2.holdStatus()).isEqualTo(PayoutHoldStatus.HELD_REPORT.name());

    var payout1 =
        generateRes.payouts().stream()
            .filter(p -> p.participationId().equals(p1.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(payout1.holdStatus()).isEqualTo(PayoutHoldStatus.NONE.name());

    PayoutGenerateResDto secondGenerate =
        winnerService.generatePayouts(operatorUser, event.getId());
    assertThat(secondGenerate.totalGenerated()).isEqualTo(0);
    assertThat(secondGenerate.heldCount()).isEqualTo(1);
    assertThat(secondGenerate.payouts()).hasSize(2);
  }

  @Test
  @DisplayName("보상 지급 후보 생성: 미확정 상태에서 호출 시 409 에러")
  void generatePayoutsWithoutFinalizingThrows() {
    winnerService.getTopN(operatorUser, round.getId(), event.getId());

    assertThatThrownBy(() -> winnerService.generatePayouts(operatorUser, event.getId()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("Top N 조회 및 확정: 추가 예외 및 엣지 케이스 분기 커버리지")
  void additionalBranchesAndEdgeCases() {
    UUID missingEventId = UUID.randomUUID();
    assertThatThrownBy(() -> winnerService.getTopN(operatorUser, round.getId(), missingEventId))
        .isInstanceOf(ResourceNotFoundException.class);

    ZoneEvent noRewardEvent =
        zoneEventRepository.save(
            ZoneEvent.builder()
                .zoneId("SUYEONG_NAMGU")
                .type(event.getType())
                .roundId(round.getId())
                .title("무보상 이벤트")
                .startsAt(round.getStartsAt())
                .durationMinutes(60)
                .status(ZoneEventStatus.ACTIVE)
                .baseReward(new RewardSnapshot(100, "POINT", null, null))
                .build());

    ZoneEvent emptyEvent =
        zoneEventRepository.save(
            ZoneEvent.builder()
                .zoneId("SUYEONG_NAMGU")
                .type(event.getType())
                .roundId(round.getId())
                .title("참여 없는 이벤트")
                .startsAt(round.getStartsAt())
                .durationMinutes(60)
                .status(ZoneEventStatus.ACTIVE)
                .baseReward(new RewardSnapshot(100, "POINT", null, null))
                .excellenceReward(new RewardSnapshot(null, "COUPON", 3, "COUPON_1"))
                .build());

    RoundTopNResDto roundTopN = winnerService.getTopN(operatorUser, round.getId(), null);
    assertThat(roundTopN.events().stream().anyMatch(e -> e.eventId().equals(noRewardEvent.getId())))
        .isFalse();
    assertThat(roundTopN.events().stream().anyMatch(e -> e.eventId().equals(emptyEvent.getId())))
        .isTrue();

    RoundTopNResDto emptyRes = winnerService.getTopN(operatorUser, round.getId(), emptyEvent.getId());
    assertThat(emptyRes.events().get(0).candidates()).isEmpty();

    assertThatThrownBy(() -> winnerService.generatePayouts(operatorUser, missingEventId))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(() -> winnerService.generatePayouts(operatorUser, noRewardEvent.getId()))
        .isInstanceOf(ResourceNotFoundException.class);

    RoundTopNResDto initial = winnerService.getTopN(operatorUser, round.getId(), event.getId());
    UUID snapshotId = initial.events().get(0).snapshotId();

    assertThatThrownBy(
            () ->
                winnerService.confirmWinners(
                    operatorUser,
                    missingEventId,
                    new ConfirmWinnersReqDto(
                        snapshotId, List.of(p2.getId()), "사유", event.getRevision())))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(
            () ->
                winnerService.confirmWinners(
                    operatorUser,
                    event.getId(),
                    new ConfirmWinnersReqDto(
                        UUID.randomUUID(), List.of(p2.getId()), "사유", event.getRevision())))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(
            () ->
                winnerService.confirmWinners(
                    operatorUser,
                    noRewardEvent.getId(),
                    new ConfirmWinnersReqDto(
                        snapshotId, List.of(p2.getId()), "사유", noRewardEvent.getRevision())))
        .isInstanceOf(ConflictException.class);

    ZoneEvent untiedEvent =
        zoneEventRepository.save(
            ZoneEvent.builder()
                .zoneId("SUYEONG_NAMGU")
                .type(event.getType())
                .roundId(round.getId())
                .title("동점 없는 이벤트")
                .startsAt(round.getStartsAt())
                .durationMinutes(60)
                .status(ZoneEventStatus.ACTIVE)
                .baseReward(new RewardSnapshot(100, "POINT", null, null))
                .excellenceReward(new RewardSnapshot(null, "COUPON", 1, "COUPON_1"))
                .build());

    ZoneEventParticipation up1 =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(untiedEvent)
                .userId(UUID.randomUUID())
                .status(ParticipationStatus.SUCCESS)
                .gpsLat(35.15)
                .gpsLng(129.11)
                .joinedAt(OffsetDateTime.now().minusHours(5))
                .visibility(ParticipationVisibility.PUBLIC)
                .build());
    up1.submit("p.jpg", "인증", 35.15, 129.11, OffsetDateTime.now().minusHours(4));
    up1.markSuccess();
    ReflectionTestUtils.setField(up1, "likeCount", 100L);
    participationRepository.save(up1);

    RoundTopNResDto untiedTopN =
        winnerService.getTopN(operatorUser, round.getId(), untiedEvent.getId());
    UUID untiedSnapshotId = untiedTopN.events().get(0).snapshotId();
    ConfirmWinnersResDto untiedConfirm =
        winnerService.confirmWinners(
            operatorUser,
            untiedEvent.getId(),
            new ConfirmWinnersReqDto(
                untiedSnapshotId, List.of(), "단독 1위 확정", untiedEvent.getRevision()));
    assertThat(untiedConfirm.finalizedParticipationIds()).containsExactly(up1.getId());

    ZoneEventRound closedRound =
        roundRepository.save(
            ZoneEventRound.builder()
                .roundNo(999)
                .startsAt(OffsetDateTime.now().minusDays(3))
                .endsAt(OffsetDateTime.now().minusDays(1))
                .build());
    closedRound.close();
    roundRepository.save(closedRound);

    ZoneEvent closedRoundEvent =
        zoneEventRepository.save(
            ZoneEvent.builder()
                .zoneId("SUYEONG_NAMGU")
                .type(event.getType())
                .roundId(closedRound.getId())
                .title("종료된 회차 이벤트")
                .startsAt(closedRound.getStartsAt())
                .durationMinutes(60)
                .status(ZoneEventStatus.ACTIVE)
                .baseReward(new RewardSnapshot(100, "POINT", null, null))
                .excellenceReward(new RewardSnapshot(null, "COUPON", 1, "COUPON_1"))
                .build());

    ZoneEventParticipation cp =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(closedRoundEvent)
                .userId(UUID.randomUUID())
                .status(ParticipationStatus.SUCCESS)
                .gpsLat(35.15)
                .gpsLng(129.11)
                .joinedAt(OffsetDateTime.now().minusHours(5))
                .visibility(ParticipationVisibility.PUBLIC)
                .build());
    cp.submit("cp.jpg", "인증", 35.15, 129.11, OffsetDateTime.now().minusHours(4));
    cp.markSuccess();
    participationRepository.save(cp);

    RoundTopNResDto closedTopN =
        winnerService.getTopN(operatorUser, closedRound.getId(), closedRoundEvent.getId());
    assertThat(closedTopN.events().get(0).closedAt()).isEqualTo(closedRound.getClosedAt());
  }

  private ZoneEventParticipation createParticipation(UUID userId, Long likeCount) {
    ZoneEventParticipation p =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(event)
                .userId(userId)
                .status(ParticipationStatus.SUCCESS)
                .gpsLat(35.15)
                .gpsLng(129.11)
                .joinedAt(OffsetDateTime.now().minusHours(5))
                .visibility(ParticipationVisibility.PUBLIC)
                .build());
    p.submit("proof-" + userId + ".jpg", "인증", 35.15, 129.11, OffsetDateTime.now().minusHours(4));
    p.markSuccess();
    ReflectionTestUtils.setField(p, "likeCount", likeCount);
    ReflectionTestUtils.setField(p, "completedAt", OffsetDateTime.now().minusHours(3));
    return participationRepository.save(p);
  }
}

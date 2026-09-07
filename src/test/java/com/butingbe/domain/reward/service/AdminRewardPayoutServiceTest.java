package com.butingbe.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.butingbe.domain.reward.dto.request.AdminRewardPayoutBulkConfirmReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutBulkScheduleReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkInfoCollectedReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkMailSentReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutMarkSentReqDto;
import com.butingbe.domain.reward.dto.request.AdminRewardPayoutPatchReqDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutDetailResDto;
import com.butingbe.domain.reward.dto.response.RewardPayoutPageResDto;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.entity.RewardPayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.exception.RewardPayoutConflictException;
import com.butingbe.domain.reward.repository.RewardCatalogRepository;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.reward.repository.RewardPayoutRepository;
import com.butingbe.domain.reward.repository.UserCouponRepository;
import com.butingbe.domain.user.entity.Name;
import com.butingbe.domain.user.entity.User;
import com.butingbe.domain.user.entity.UserRole;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.RoundStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventRound;
import com.butingbe.domain.zoneevent.entity.ZoneEventStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventType;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRoundRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventTypeRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.support.AbstractContainerTest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminRewardPayoutServiceTest extends AbstractContainerTest {

  @Autowired private AdminRewardPayoutService payoutService;
  @Autowired private RewardPayoutRepository payoutRepository;
  @Autowired private RewardCatalogRepository catalogRepository;
  @Autowired private RewardGrantRepository grantRepository;
  @Autowired private UserCouponRepository userCouponRepository;
  @Autowired private ZoneEventRoundRepository roundRepository;
  @Autowired private ZoneEventRepository eventRepository;
  @Autowired private ZoneEventTypeRepository typeRepository;
  @Autowired private ZoneEventParticipationRepository participationRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private UserPointService userPointService;

  private User testUser;
  private User adminUser;
  private ZoneEventRound round;
  private ZoneEvent event;
  private ZoneEventParticipation participation;
  private RewardCatalog couponCatalog;

  @BeforeEach
  void setUp() {
    testUser =
        userRepository.save(
            User.builder()
                .email("user-" + UUID.randomUUID() + "@test.com")
                .provider("google")
                .providerId("google-" + UUID.randomUUID())
                .name(new Name("Kim", "Tester"))
                .nickname("tester")
                .role(UserRole.USER)
                .build());

    adminUser =
        userRepository.save(
            User.builder()
                .email("admin-" + UUID.randomUUID() + "@test.com")
                .provider("google")
                .providerId("google-" + UUID.randomUUID())
                .name(new Name("Admin", "User"))
                .nickname("admin")
                .role(UserRole.ADMIN)
                .build());

    ZoneEventType type =
        typeRepository.save(
            ZoneEventType.builder()
                .typeCode("TYPE-" + UUID.randomUUID().toString().substring(0, 8))
                .name("미션")
                .requiresUpload(true)
                .build());

    round =
        roundRepository.save(
            ZoneEventRound.builder()
                .startsAt(OffsetDateTime.now().minusDays(2))
                .endsAt(OffsetDateTime.now().plusDays(5))
                .status(RoundStatus.OPEN)
                .build());

    event =
        eventRepository.save(
            ZoneEvent.builder()
                .zoneId("SUYEONG_NAMGU")
                .type(type)
                .roundId(round.getId())
                .title("테스트 이벤트")
                .description("설명")
                .startsAt(OffsetDateTime.now().minusHours(1))
                .durationMinutes(1440)
                .status(ZoneEventStatus.ACTIVE)
                .baseReward(new RewardSnapshot(50, "BADGE_TEST", null, null))
                .successLimitPerUser(1)
                .build());

    participation =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(event)
                .userId(testUser.getId())
                .status(ParticipationStatus.SUCCESS)
                .gpsLat(35.1)
                .gpsLng(129.1)
                .joinedAt(OffsetDateTime.now())
                .visibility(ParticipationVisibility.PUBLIC)
                .build());

    if (!catalogRepository.existsByCode("POINT_BASE")) {
      catalogRepository.save(
          RewardCatalog.builder()
              .rewardType(RewardType.POINT)
              .code("POINT_BASE")
              .name("기본 포인트")
              .pointAmount(50)
              .build());
    }

    couponCatalog =
        catalogRepository.save(
            RewardCatalog.builder()
                .rewardType(RewardType.COUPON)
                .code("COUPON-" + UUID.randomUUID().toString().substring(0, 8))
                .name("스타벅스 아메리카노")
                .stock(10)
                .validDays(30)
                .build());
  }

  @Test
  @DisplayName("목록 조회 및 필터링이 정상 동작한다")
  void listWithFilters() {
    RewardPayout p1 =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, "BADGE_A");

    RewardPayoutPageResDto result =
        payoutService.list(
            round.getId(),
            event.getId(),
            GrantReason.BASE,
            RewardPayoutStatus.PENDING_CONFIRM,
            RewardPayoutHoldStatus.NONE,
            null,
            null,
            0,
            10);

    assertThat(result.totalElements()).isGreaterThanOrEqualTo(1);
    assertThat(result.items()).anyMatch(item -> item.payoutId().equals(p1.getId()));
  }

  @Test
  @DisplayName("상세 조회 시 지급 정보와 변경 이력 목록을 반환한다")
  void detail() {
    RewardPayout payout =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 100, "BADGE_B");

    RewardPayoutDetailResDto detail = payoutService.detail(payout.getId());

    assertThat(detail.payoutId()).isEqualTo(payout.getId());
    assertThat(detail.snapshot().points()).isEqualTo(100);
    assertThat(detail.snapshot().badgeCode()).isEqualTo("BADGE_B");
    assertThat(detail.histories()).isNotEmpty();
  }

  @Test
  @DisplayName("확정 전 항목 수정이 정상 반영된다")
  void patchBeforeConfirm() {
    RewardPayout payout =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);

    AdminRewardPayoutPatchReqDto req =
        new AdminRewardPayoutPatchReqDto(
            null, null, null, 150, "BADGE_NEW", null, "보상 상향", payout.getRevision());

    RewardPayoutDetailResDto updated = payoutService.patch(adminUser.getId(), payout.getId(), req);

    assertThat(updated.snapshot().points()).isEqualTo(150);
    assertThat(updated.snapshot().badgeCode()).isEqualTo("BADGE_NEW");
    assertThat(updated.note()).isEqualTo("보상 상향");
    assertThat(updated.revision()).isEqualTo(1L);
  }

  @Test
  @DisplayName("기본 보상 확정 시 즉시 포인트 및 배지가 원장에 원자적으로 반영된다")
  void bulkConfirmBaseImmediateSettlement() {
    RewardPayout payout =
        payoutService.createBasePayout(
            round.getId(),
            event.getId(),
            participation.getId(),
            testUser.getId(),
            75,
            "BADGE_SETTLE");

    AdminRewardPayoutBulkConfirmReqDto req =
        new AdminRewardPayoutBulkConfirmReqDto(List.of(payout.getId()), Map.of(payout.getId(), 0L));

    List<UUID> confirmed = payoutService.bulkConfirm(adminUser.getId(), req);

    assertThat(confirmed).containsExactly(payout.getId());

    RewardPayout reloaded = payoutRepository.findById(payout.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(RewardPayoutStatus.PAID);
    assertThat(reloaded.getPaidAt()).isNotNull();

    int balance = userPointService.getBalance(testUser.getId());
    assertThat(balance).isEqualTo(75);

    boolean hasGrant =
        grantRepository.existsByParticipationIdAndGrantReason(
            participation.getId(), GrantReason.BASE);
    assertThat(hasGrant).isTrue();
  }

  @Test
  @DisplayName("하나라도 보류 상태이면 일괄 확정은 원자적으로 실패하고 문제 ID를 반환한다")
  void bulkConfirmFailsIfAnyHeld() {
    RewardPayout p1 =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);

    ZoneEventParticipation part2 =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(event)
                .userId(testUser.getId())
                .status(ParticipationStatus.SUCCESS)
                .gpsLat(35.1)
                .gpsLng(129.1)
                .joinedAt(OffsetDateTime.now())
                .visibility(ParticipationVisibility.PUBLIC)
                .build());

    RewardPayout p2 =
        payoutService.createBasePayout(
            round.getId(), event.getId(), part2.getId(), testUser.getId(), 50, null);
    payoutService.hold(adminUser.getId(), p2.getId(), "신고 접수");

    AdminRewardPayoutBulkConfirmReqDto req =
        new AdminRewardPayoutBulkConfirmReqDto(List.of(p1.getId(), p2.getId()), null);

    assertThatThrownBy(() -> payoutService.bulkConfirm(adminUser.getId(), req))
        .isInstanceOf(RewardPayoutConflictException.class)
        .satisfies(
            ex -> {
              RewardPayoutConflictException conflict = (RewardPayoutConflictException) ex;
              assertThat(conflict.getProblematicPayoutIds()).contains(p2.getId());
            });

    RewardPayout p1Reloaded = payoutRepository.findById(p1.getId()).orElseThrow();
    assertThat(p1Reloaded.getStatus()).isEqualTo(RewardPayoutStatus.PENDING_CONFIRM);
  }

  @Test
  @DisplayName("특별 보상의 전체 배송 라이프사이클과 원장 반영이 정상 동작한다")
  void topLikeLifecycleAndGrant() {
    RewardPayout payout =
        payoutService.createTopLikePayout(
            round.getId(),
            event.getId(),
            participation.getId(),
            testUser.getId(),
            1,
            25,
            couponCatalog,
            couponCatalog.getName());

    payoutService.bulkConfirm(
        adminUser.getId(), new AdminRewardPayoutBulkConfirmReqDto(List.of(payout.getId()), null));
    RewardPayout confirmed = payoutRepository.findById(payout.getId()).orElseThrow();
    assertThat(confirmed.getStatus()).isEqualTo(RewardPayoutStatus.CONFIRMED);

    assertThatThrownBy(
            () ->
                payoutService.patch(
                    adminUser.getId(),
                    payout.getId(),
                    new AdminRewardPayoutPatchReqDto(
                        null, null, null, 999, null, null, null, null)))
        .isInstanceOf(ConflictException.class);

    payoutService.markMailSent(
        adminUser.getId(),
        new AdminRewardPayoutMarkMailSentReqDto(
            List.of(payout.getId()), OffsetDateTime.now(), "메일 발송 완료", null));
    assertThat(payoutRepository.findById(payout.getId()).orElseThrow().getStatus())
        .isEqualTo(RewardPayoutStatus.MAIL_SENT);

    payoutService.markInfoCollected(
        adminUser.getId(),
        new AdminRewardPayoutMarkInfoCollectedReqDto(
            List.of(payout.getId()), OffsetDateTime.now(), "수령인 주소 확인", null));
    assertThat(payoutRepository.findById(payout.getId()).orElseThrow().getStatus())
        .isEqualTo(RewardPayoutStatus.INFO_COLLECTED);

    payoutService.markSent(
        adminUser.getId(),
        new AdminRewardPayoutMarkSentReqDto(
            List.of(payout.getId()), OffsetDateTime.now(), "CJ-123456789", "발송 완료", null));

    RewardPayout sent = payoutRepository.findById(payout.getId()).orElseThrow();
    assertThat(sent.getStatus()).isEqualTo(RewardPayoutStatus.SENT);
    assertThat(sent.getReference()).isEqualTo("CJ-123456789");

    boolean grantExists =
        grantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
            participation.getId(), GrantReason.TOP_LIKE, couponCatalog.getId());
    assertThat(grantExists).isTrue();

    boolean couponExists =
        userCouponRepository.findAll().stream()
            .anyMatch(
                c ->
                    c.getUserId().equals(testUser.getId())
                        && c.getReward().getId().equals(couponCatalog.getId()));
    assertThat(couponExists).isTrue();
  }

  @Test
  @DisplayName("일정 변경 및 재시도가 정상 동작한다")
  void scheduleAndRetry() {
    RewardPayout payout =
        payoutService.createBasePayout(
            round.getId(),
            event.getId(),
            participation.getId(),
            testUser.getId(),
            50,
            "BADGE_RETRY");

    OffsetDateTime targetDate = OffsetDateTime.now().plusDays(3);
    payoutService.bulkSchedule(
        adminUser.getId(),
        new AdminRewardPayoutBulkScheduleReqDto(List.of(payout.getId()), targetDate, null));

    RewardPayout scheduled = payoutRepository.findById(payout.getId()).orElseThrow();
    assertThat(scheduled.getScheduledAt()).isEqualTo(targetDate);

    RewardPayoutDetailResDto retried = payoutService.retry(adminUser.getId(), payout.getId());
    assertThat(retried.status()).isEqualTo(RewardPayoutStatus.PAID);
    assertThat(retried.retryCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("목록 필터링 시 scheduledFrom 및 scheduledTo 조건이 적용된다")
  void listWithDateFilters() {
    OffsetDateTime now = OffsetDateTime.now();
    RewardPayout payout =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);
    payoutService.bulkSchedule(
        adminUser.getId(),
        new AdminRewardPayoutBulkScheduleReqDto(List.of(payout.getId()), now.plusDays(2), null));

    RewardPayoutPageResDto filtered =
        payoutService.list(null, null, null, null, null, now.minusDays(1), now.plusDays(5), 0, 10);
    assertThat(filtered.items()).isNotEmpty();
  }

  @Test
  @DisplayName("수정 시 버전 충돌 및 카탈로그 변경 분기가 정상 동작한다")
  void patchVariationsAndConflict() {
    RewardPayout payout =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);

    assertThatThrownBy(
            () ->
                payoutService.patch(
                    adminUser.getId(),
                    payout.getId(),
                    new AdminRewardPayoutPatchReqDto(
                        null, null, null, null, null, null, null, 999L)))
        .isInstanceOf(ConflictException.class);

    RewardPayoutDetailResDto patchedWithCatalogId =
        payoutService.patch(
            adminUser.getId(),
            payout.getId(),
            new AdminRewardPayoutPatchReqDto(
                couponCatalog.getId(),
                null,
                null,
                null,
                null,
                null,
                "카탈로그 ID로 수정",
                payout.getRevision()));
    assertThat(patchedWithCatalogId.snapshot().code()).isEqualTo(couponCatalog.getCode());

    RewardPayoutDetailResDto patchedWithCatalogCode =
        payoutService.patch(
            adminUser.getId(),
            payout.getId(),
            new AdminRewardPayoutPatchReqDto(
                null,
                couponCatalog.getCode(),
                null,
                null,
                null,
                null,
                "카탈로그 코드로 수정",
                patchedWithCatalogId.revision()));
    assertThat(patchedWithCatalogCode.snapshot().code()).isEqualTo(couponCatalog.getCode());
  }

  @Test
  @DisplayName("일괄 확정 시 다양한 충돌 조건들이 원자적으로 거절된다")
  void bulkConfirmConflictVariations() {
    UUID nonExistentId = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                payoutService.bulkConfirm(
                    adminUser.getId(),
                    new AdminRewardPayoutBulkConfirmReqDto(List.of(nonExistentId), null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    RewardPayout p1 =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);
    assertThatThrownBy(
            () ->
                payoutService.bulkConfirm(
                    adminUser.getId(),
                    new AdminRewardPayoutBulkConfirmReqDto(
                        List.of(p1.getId()), Map.of(p1.getId(), 999L))))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.bulkConfirm(
        adminUser.getId(), new AdminRewardPayoutBulkConfirmReqDto(List.of(p1.getId()), null));
    assertThatThrownBy(
            () ->
                payoutService.bulkConfirm(
                    adminUser.getId(),
                    new AdminRewardPayoutBulkConfirmReqDto(List.of(p1.getId()), null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    ZoneEventParticipation partFailed =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(event)
                .userId(testUser.getId())
                .status(ParticipationStatus.FAIL)
                .gpsLat(35.1)
                .gpsLng(129.1)
                .joinedAt(OffsetDateTime.now())
                .visibility(ParticipationVisibility.PUBLIC)
                .build());
    RewardPayout pFailed =
        payoutService.createBasePayout(
            round.getId(), event.getId(), partFailed.getId(), testUser.getId(), 50, null);
    assertThatThrownBy(
            () ->
                payoutService.bulkConfirm(
                    adminUser.getId(),
                    new AdminRewardPayoutBulkConfirmReqDto(List.of(pFailed.getId()), null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    RewardPayout pTopLikeNoPrize =
        payoutRepository.save(
            RewardPayout.builder()
                .roundId(round.getId())
                .eventId(event.getId())
                .participationId(participation.getId())
                .userId(testUser.getId())
                .rewardReason(GrantReason.TOP_LIKE)
                .status(RewardPayoutStatus.PENDING_CONFIRM)
                .holdStatus(RewardPayoutHoldStatus.NONE)
                .rewardCatalog(null)
                .prizeName(null)
                .build());
    assertThatThrownBy(
            () ->
                payoutService.bulkConfirm(
                    adminUser.getId(),
                    new AdminRewardPayoutBulkConfirmReqDto(List.of(pTopLikeNoPrize.getId()), null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    ZoneEventParticipation partFuture =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(event)
                .userId(testUser.getId())
                .status(ParticipationStatus.SUCCESS)
                .gpsLat(35.1)
                .gpsLng(129.1)
                .joinedAt(OffsetDateTime.now())
                .visibility(ParticipationVisibility.PUBLIC)
                .build());
    RewardPayout pFuture =
        payoutService.createBasePayout(
            round.getId(), event.getId(), partFuture.getId(), testUser.getId(), 50, null);
    pFuture.schedule(OffsetDateTime.now().plusDays(5));
    payoutRepository.save(pFuture);
    payoutService.bulkConfirm(
        adminUser.getId(), new AdminRewardPayoutBulkConfirmReqDto(List.of(pFuture.getId()), null));
    assertThat(payoutRepository.findById(pFuture.getId()).orElseThrow().getStatus())
        .isEqualTo(RewardPayoutStatus.CONFIRMED);
  }

  @Test
  @DisplayName("일정 변경 시 충돌 조건이 원자적으로 거절된다")
  void bulkScheduleConflictVariations() {
    UUID nonExistentId = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                payoutService.bulkSchedule(
                    adminUser.getId(),
                    new AdminRewardPayoutBulkScheduleReqDto(
                        List.of(nonExistentId), OffsetDateTime.now(), null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    RewardPayout p =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);
    assertThatThrownBy(
            () ->
                payoutService.bulkSchedule(
                    adminUser.getId(),
                    new AdminRewardPayoutBulkScheduleReqDto(
                        List.of(p.getId()), OffsetDateTime.now(), Map.of(p.getId(), 999L))))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.hold(adminUser.getId(), p.getId(), "보류 설정");
    assertThatThrownBy(
            () ->
                payoutService.bulkSchedule(
                    adminUser.getId(),
                    new AdminRewardPayoutBulkScheduleReqDto(
                        List.of(p.getId()), OffsetDateTime.now(), null)))
        .isInstanceOf(RewardPayoutConflictException.class);
  }

  @Test
  @DisplayName("메일 발송, 정보 수집, 발송 완료 시의 충돌 조건들이 원자적으로 검증된다")
  void markStepsConflictVariations() {
    UUID nonExistentId = UUID.randomUUID();
    RewardPayout basePayout =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);

    assertThatThrownBy(
            () ->
                payoutService.markMailSent(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkMailSentReqDto(
                        List.of(nonExistentId, basePayout.getId()),
                        OffsetDateTime.now(),
                        "메일",
                        null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    RewardPayout topLike =
        payoutService.createTopLikePayout(
            round.getId(),
            event.getId(),
            participation.getId(),
            testUser.getId(),
            1,
            10,
            couponCatalog,
            "스타벅스");

    assertThatThrownBy(
            () ->
                payoutService.markMailSent(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkMailSentReqDto(
                        List.of(topLike.getId()), OffsetDateTime.now(), "메일", null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.bulkConfirm(
        adminUser.getId(), new AdminRewardPayoutBulkConfirmReqDto(List.of(topLike.getId()), null));

    payoutService.hold(adminUser.getId(), topLike.getId(), "보류");
    assertThatThrownBy(
            () ->
                payoutService.markMailSent(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkMailSentReqDto(
                        List.of(topLike.getId()), OffsetDateTime.now(), "메일", null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.releaseHold(adminUser.getId(), topLike.getId(), "해제");

    assertThatThrownBy(
            () ->
                payoutService.markInfoCollected(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkInfoCollectedReqDto(
                        List.of(topLike.getId()), OffsetDateTime.now(), "정보수집", null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    assertThatThrownBy(
            () ->
                payoutService.markMailSent(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkMailSentReqDto(
                        List.of(topLike.getId()),
                        OffsetDateTime.now(),
                        "메일",
                        Map.of(topLike.getId(), 999L))))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.markMailSent(
        adminUser.getId(),
        new AdminRewardPayoutMarkMailSentReqDto(
            List.of(topLike.getId()), OffsetDateTime.now(), "메일 발송", null));

    assertThatThrownBy(
            () ->
                payoutService.markSent(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkSentReqDto(
                        List.of(topLike.getId()), OffsetDateTime.now(), "REF", "발송", null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    assertThatThrownBy(
            () ->
                payoutService.markInfoCollected(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkInfoCollectedReqDto(
                        List.of(nonExistentId, basePayout.getId()),
                        OffsetDateTime.now(),
                        "정보수집",
                        null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    assertThatThrownBy(
            () ->
                payoutService.markInfoCollected(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkInfoCollectedReqDto(
                        List.of(topLike.getId()),
                        OffsetDateTime.now(),
                        "정보수집",
                        Map.of(topLike.getId(), 999L))))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.hold(adminUser.getId(), topLike.getId(), "보류");
    assertThatThrownBy(
            () ->
                payoutService.markInfoCollected(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkInfoCollectedReqDto(
                        List.of(topLike.getId()), OffsetDateTime.now(), "정보수집", null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.releaseHold(adminUser.getId(), topLike.getId(), "해제");
    payoutService.markInfoCollected(
        adminUser.getId(),
        new AdminRewardPayoutMarkInfoCollectedReqDto(
            List.of(topLike.getId()), OffsetDateTime.now(), "정보수집완료", null));

    assertThatThrownBy(
            () ->
                payoutService.markSent(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkSentReqDto(
                        List.of(nonExistentId, basePayout.getId()),
                        OffsetDateTime.now(),
                        "REF",
                        "발송",
                        null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    assertThatThrownBy(
            () ->
                payoutService.markSent(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkSentReqDto(
                        List.of(topLike.getId()),
                        OffsetDateTime.now(),
                        "REF",
                        "발송",
                        Map.of(topLike.getId(), 999L))))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.hold(adminUser.getId(), topLike.getId(), "보류");
    assertThatThrownBy(
            () ->
                payoutService.markSent(
                    adminUser.getId(),
                    new AdminRewardPayoutMarkSentReqDto(
                        List.of(topLike.getId()), OffsetDateTime.now(), "REF", "발송", null)))
        .isInstanceOf(RewardPayoutConflictException.class);

    payoutService.releaseHold(adminUser.getId(), topLike.getId(), "해제");
    payoutService.markSent(
        adminUser.getId(),
        new AdminRewardPayoutMarkSentReqDto(
            List.of(topLike.getId()), OffsetDateTime.now(), "REF-123", "발송완료", null));
  }

  @Test
  @DisplayName("재시도 충돌 및 특별보상 재시도, 멱등성이 정상 동작한다")
  void retryAndIdempotencyVariations() {
    RewardPayout payout =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);

    payoutService.hold(adminUser.getId(), payout.getId(), "보류");
    assertThatThrownBy(() -> payoutService.retry(adminUser.getId(), payout.getId()))
        .isInstanceOf(ConflictException.class);

    payoutService.releaseHold(adminUser.getId(), payout.getId(), "해제");
    payoutService.retry(adminUser.getId(), payout.getId());

    assertThatThrownBy(() -> payoutService.retry(adminUser.getId(), payout.getId()))
        .isInstanceOf(ConflictException.class);

    RewardCatalog noValidDaysCatalog =
        catalogRepository.save(
            RewardCatalog.builder()
                .rewardType(RewardType.COUPON)
                .code("NO_VALID_DAYS-" + UUID.randomUUID().toString().substring(0, 8))
                .name("기간 무제한 쿠폰")
                .stock(5)
                .validDays(null)
                .build());

    ZoneEventParticipation partTop =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(event)
                .userId(testUser.getId())
                .status(ParticipationStatus.SUCCESS)
                .gpsLat(35.1)
                .gpsLng(129.1)
                .joinedAt(OffsetDateTime.now())
                .visibility(ParticipationVisibility.PUBLIC)
                .build());

    RewardPayout topLike =
        payoutService.createTopLikePayout(
            round.getId(),
            event.getId(),
            partTop.getId(),
            testUser.getId(),
            1,
            10,
            noValidDaysCatalog,
            "쿠폰");
    payoutService.bulkConfirm(
        adminUser.getId(), new AdminRewardPayoutBulkConfirmReqDto(List.of(topLike.getId()), null));
    RewardPayoutDetailResDto topRetried = payoutService.retry(adminUser.getId(), topLike.getId());
    assertThat(topRetried.status()).isEqualTo(RewardPayoutStatus.SENT);

    RewardPayoutDetailResDto topRetriedAgain =
        payoutService.retry(adminUser.getId(), topLike.getId());
    assertThat(topRetriedAgain.status()).isEqualTo(RewardPayoutStatus.SENT);

    ZoneEventParticipation partPhysical =
        participationRepository.save(
            ZoneEventParticipation.builder()
                .event(event)
                .userId(testUser.getId())
                .status(ParticipationStatus.SUCCESS)
                .gpsLat(35.1)
                .gpsLng(129.1)
                .joinedAt(OffsetDateTime.now())
                .visibility(ParticipationVisibility.PUBLIC)
                .build());
    RewardPayout physicalPayout =
        payoutService.createTopLikePayout(
            round.getId(),
            event.getId(),
            partPhysical.getId(),
            testUser.getId(),
            1,
            10,
            null,
            "실물 경품");
    payoutService.bulkConfirm(
        adminUser.getId(),
        new AdminRewardPayoutBulkConfirmReqDto(List.of(physicalPayout.getId()), null));
    payoutService.markMailSent(
        adminUser.getId(),
        new AdminRewardPayoutMarkMailSentReqDto(
            List.of(physicalPayout.getId()), OffsetDateTime.now(), "메일", null));
    payoutService.markInfoCollected(
        adminUser.getId(),
        new AdminRewardPayoutMarkInfoCollectedReqDto(
            List.of(physicalPayout.getId()), OffsetDateTime.now(), "정보수집", null));
    payoutService.markSent(
        adminUser.getId(),
        new AdminRewardPayoutMarkSentReqDto(
            List.of(physicalPayout.getId()), OffsetDateTime.now(), "CJ-PHYSICAL", "발송완료", null));
    RewardPayout physicalSent = payoutRepository.findById(physicalPayout.getId()).orElseThrow();
    assertThat(physicalSent.getStatus()).isEqualTo(RewardPayoutStatus.SENT);

    RewardPayout sameBase =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);
    assertThat(sameBase.getId()).isEqualTo(payout.getId());

    RewardPayout sameTop =
        payoutService.createTopLikePayout(
            round.getId(),
            event.getId(),
            partTop.getId(),
            testUser.getId(),
            1,
            10,
            noValidDaysCatalog,
            "쿠폰");
    assertThat(sameTop.getId()).isEqualTo(topLike.getId());
  }

  @Test
  @DisplayName("존재하지 않는 리소스에 대한 예외가 정상 발생한다")
  void notFoundExceptions() {
    UUID nonExistentId = UUID.randomUUID();
    assertThatThrownBy(() -> payoutService.detail(nonExistentId))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(
            () ->
                payoutService.patch(
                    adminUser.getId(),
                    nonExistentId,
                    new AdminRewardPayoutPatchReqDto(
                        null, null, null, null, null, null, null, null)))
        .isInstanceOf(ResourceNotFoundException.class);

    RewardPayout payout =
        payoutService.createBasePayout(
            round.getId(), event.getId(), participation.getId(), testUser.getId(), 50, null);

    assertThatThrownBy(
            () ->
                payoutService.patch(
                    adminUser.getId(),
                    payout.getId(),
                    new AdminRewardPayoutPatchReqDto(
                        UUID.randomUUID(), null, null, null, null, null, null, null)))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(
            () ->
                payoutService.patch(
                    adminUser.getId(),
                    payout.getId(),
                    new AdminRewardPayoutPatchReqDto(
                        null, "NON_EXISTENT_CODE", null, null, null, null, null, null)))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(() -> payoutService.retry(adminUser.getId(), nonExistentId))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(() -> payoutService.hold(adminUser.getId(), nonExistentId, "note"))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThatThrownBy(() -> payoutService.releaseHold(adminUser.getId(), nonExistentId, "note"))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}

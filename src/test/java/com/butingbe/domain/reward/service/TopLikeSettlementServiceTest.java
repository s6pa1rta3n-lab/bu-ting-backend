package com.butingbe.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.butingbe.domain.reward.dto.response.SettlementItemStatus;
import com.butingbe.domain.reward.dto.response.TopLikeSettlementReportResDto;
import com.butingbe.domain.reward.entity.CouponStatus;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.entity.UserCoupon;
import com.butingbe.domain.reward.repository.RewardCatalogRepository;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.reward.repository.UserBadgeRepository;
import com.butingbe.domain.reward.repository.UserCouponRepository;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TopLikeSettlementServiceTest {

  @Mock private ZoneEventRepository zoneEventRepository;
  @Mock private ZoneEventParticipationRepository participationRepository;
  @Mock private RewardCatalogRepository rewardCatalogRepository;
  @Mock private RewardGrantRepository rewardGrantRepository;
  @Mock private UserCouponRepository userCouponRepository;
  @Mock private UserBadgeRepository userBadgeRepository;
  @Mock private UserPointService userPointService;

  private TopLikeSettlementService settlementService;

  private final UUID eventId = UUID.randomUUID();
  private final UUID roundId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID participationId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    settlementService =
        new TopLikeSettlementService(
            zoneEventRepository,
            participationRepository,
            rewardCatalogRepository,
            rewardGrantRepository,
            userCouponRepository,
            userBadgeRepository,
            userPointService,
            new SettlementLockManager());
  }

  @Test
  @DisplayName("존재하지 않는 이벤트 정산 시 ResourceNotFoundException이 발생한다")
  void eventNotFound() {
    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> settlementService.settleEvent(eventId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("error.zone_event.not_found");
  }

  @Test
  @DisplayName("우수 보상 설정이 없거나 topN이 0 이하이면 빈 리포트를 반환한다")
  void excellenceRewardNullOrTopNZero() {
    ZoneEvent eventWithoutReward = ZoneEvent.builder().excellenceReward(null).build();
    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.of(eventWithoutReward));

    TopLikeSettlementReportResDto report = settlementService.settleEvent(eventId);
    assertThat(report.totalCandidates()).isEqualTo(0);
    assertThat(report.totalGranted()).isEqualTo(0);

    RewardSnapshot zeroTopN = new RewardSnapshot(null, null, 0, null);
    ZoneEvent eventWithZeroTopN = ZoneEvent.builder().excellenceReward(zeroTopN).build();
    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.of(eventWithZeroTopN));

    report = settlementService.settleEvent(eventId);
    assertThat(report.totalCandidates()).isEqualTo(0);
  }

  @Test
  @DisplayName("상위 N명에게 포인트, 배지, 실물 쿠폰이 정상적으로 정산 지급된다")
  void settleEventSuccess() {
    RewardSnapshot snapshot = new RewardSnapshot(50, "BADGE_TOP", 1, "CPN_COFFEE");
    ZoneEvent event = ZoneEvent.builder().excellenceReward(snapshot).roundId(roundId).build();

    ZoneEventParticipation candidate =
        ZoneEventParticipation.builder()
            .userId(userId)
            .status(ParticipationStatus.SUCCESS)
            .visibility(ParticipationVisibility.PUBLIC)
            .likeCount(15L)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", participationId);

    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(participationRepository.findTopCandidates(
            eventId,
            ParticipationStatus.SUCCESS,
            ParticipationVisibility.PUBLIC,
            PageRequest.of(0, 1)))
        .thenReturn(List.of(candidate));

    RewardCatalog pointCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.POINT)
            .code("POINT_BASE")
            .pointAmount(50)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(
        pointCatalog, "id", UUID.randomUUID());

    RewardCatalog badgeCatalog =
        RewardCatalog.builder().rewardType(RewardType.BADGE).code("BADGE_TOP").build();
    org.springframework.test.util.ReflectionTestUtils.setField(
        badgeCatalog, "id", UUID.randomUUID());

    RewardCatalog prizeCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN_COFFEE")
            .stock(10)
            .validDays(30)
            .active(true)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(
        prizeCatalog, "id", UUID.randomUUID());

    when(rewardCatalogRepository.findByCode("POINT_BASE")).thenReturn(Optional.of(pointCatalog));
    when(rewardCatalogRepository.findByCode("BADGE_TOP")).thenReturn(Optional.of(badgeCatalog));
    when(rewardCatalogRepository.findByCode("CPN_COFFEE")).thenReturn(Optional.of(prizeCatalog));

    when(rewardGrantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
            eq(participationId), eq(GrantReason.TOP_LIKE), any()))
        .thenReturn(false);

    UUID grantId = UUID.randomUUID();
    RewardGrant mockGrant =
        RewardGrant.builder()
            .userId(userId)
            .reward(prizeCatalog)
            .participationId(participationId)
            .grantReason(GrantReason.TOP_LIKE)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(mockGrant, "id", grantId);
    when(rewardGrantRepository.save(any())).thenReturn(mockGrant);

    UserCoupon mockCoupon =
        UserCoupon.builder()
            .userId(userId)
            .reward(prizeCatalog)
            .grantId(grantId)
            .couponCode("CPN-1234567890123456")
            .status(CouponStatus.ISSUED)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(mockCoupon, "id", UUID.randomUUID());
    when(userCouponRepository.save(any())).thenReturn(mockCoupon);

    TopLikeSettlementReportResDto report = settlementService.settleEvent(eventId);

    assertThat(report.totalCandidates()).isEqualTo(1);
    assertThat(report.totalGranted()).isEqualTo(1);
    assertThat(report.totalSkippedStock()).isEqualTo(0);
    assertThat(report.totalSkippedMonthlyCap()).isEqualTo(0);
    assertThat(report.items().get(0).status()).isEqualTo(SettlementItemStatus.GRANTED);
    assertThat(prizeCatalog.getStock()).isEqualTo(9);

    verify(userPointService).record(eq(userId), eq(50), eq(GrantReason.TOP_LIKE.name()), any());
    verify(userBadgeRepository).save(any());
  }

  @Test
  @DisplayName("실물 보상 카탈로그가 없거나 비활성화인 경우 SKIPPED_NO_REWARD 처리된다")
  void settleMissingPrizeCatalog() {
    RewardSnapshot snapshot = new RewardSnapshot(null, null, 1, "CPN_MISSING");
    ZoneEvent event = ZoneEvent.builder().excellenceReward(snapshot).build();

    ZoneEventParticipation candidate =
        ZoneEventParticipation.builder()
            .userId(userId)
            .status(ParticipationStatus.SUCCESS)
            .visibility(ParticipationVisibility.PUBLIC)
            .likeCount(5L)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", participationId);

    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(participationRepository.findTopCandidates(any(), any(), any(), any()))
        .thenReturn(List.of(candidate));
    when(rewardCatalogRepository.findByCode("CPN_MISSING")).thenReturn(Optional.empty());

    TopLikeSettlementReportResDto report = settlementService.settleEvent(eventId);
    assertThat(report.items().get(0).status()).isEqualTo(SettlementItemStatus.SKIPPED_NO_REWARD);
  }

  @Test
  @DisplayName("이미 정산된 참여에 대해서는 ALREADY_GRANTED 상태로 멱등 처리되며 재고가 차감되지 않는다")
  void settleAlreadyGranted() {
    RewardSnapshot snapshot = new RewardSnapshot(null, null, 1, "CPN_COFFEE");
    ZoneEvent event = ZoneEvent.builder().excellenceReward(snapshot).build();

    ZoneEventParticipation candidate =
        ZoneEventParticipation.builder()
            .userId(userId)
            .status(ParticipationStatus.SUCCESS)
            .visibility(ParticipationVisibility.PUBLIC)
            .likeCount(5L)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", participationId);

    RewardCatalog prizeCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN_COFFEE")
            .stock(10)
            .active(true)
            .build();
    UUID rewardId = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(prizeCatalog, "id", rewardId);

    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(participationRepository.findTopCandidates(any(), any(), any(), any()))
        .thenReturn(List.of(candidate));
    when(rewardCatalogRepository.findByCode("CPN_COFFEE")).thenReturn(Optional.of(prizeCatalog));
    when(rewardGrantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
            participationId, GrantReason.TOP_LIKE, rewardId))
        .thenReturn(true);

    UserCoupon existingCoupon = UserCoupon.builder().userId(userId).reward(prizeCatalog).build();
    when(userCouponRepository.findByUserIdOrderByIssuedAtDesc(userId))
        .thenReturn(List.of(existingCoupon));

    TopLikeSettlementReportResDto report = settlementService.settleEvent(eventId);
    assertThat(report.items().get(0).status()).isEqualTo(SettlementItemStatus.ALREADY_GRANTED);
    assertThat(report.totalGranted()).isEqualTo(1);
    assertThat(prizeCatalog.getStock()).isEqualTo(10);
  }

  @Test
  @DisplayName("월 캡이 초과된 경우 SKIPPED_MONTHLY_CAP 리포트를 반환한다")
  void settleMonthlyCapExceeded() {
    RewardSnapshot snapshot = new RewardSnapshot(null, null, 1, "CPN_COFFEE");
    ZoneEvent event = ZoneEvent.builder().excellenceReward(snapshot).build();

    ZoneEventParticipation candidate =
        ZoneEventParticipation.builder()
            .userId(userId)
            .status(ParticipationStatus.SUCCESS)
            .visibility(ParticipationVisibility.PUBLIC)
            .likeCount(5L)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", participationId);

    RewardCatalog prizeCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN_COFFEE")
            .monthlyCap(5)
            .stock(10)
            .active(true)
            .build();
    UUID rewardId = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(prizeCatalog, "id", rewardId);

    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(participationRepository.findTopCandidates(any(), any(), any(), any()))
        .thenReturn(List.of(candidate));
    when(rewardCatalogRepository.findByCode("CPN_COFFEE")).thenReturn(Optional.of(prizeCatalog));
    when(rewardGrantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
            participationId, GrantReason.TOP_LIKE, rewardId))
        .thenReturn(false);
    when(rewardGrantRepository.countActiveGrantsInMonth(eq(rewardId), any(), any())).thenReturn(5L);

    TopLikeSettlementReportResDto report = settlementService.settleEvent(eventId);
    assertThat(report.totalSkippedMonthlyCap()).isEqualTo(1);
    assertThat(report.items().get(0).status()).isEqualTo(SettlementItemStatus.SKIPPED_MONTHLY_CAP);
    assertThat(prizeCatalog.getStock()).isEqualTo(10);
  }

  @Test
  @DisplayName("재고가 소진된 경우 SKIPPED_OUT_OF_STOCK 리포트를 반환한다")
  void settleOutOfStock() {
    RewardSnapshot snapshot = new RewardSnapshot(null, null, 1, "CPN_COFFEE");
    ZoneEvent event = ZoneEvent.builder().excellenceReward(snapshot).build();

    ZoneEventParticipation candidate =
        ZoneEventParticipation.builder()
            .userId(userId)
            .status(ParticipationStatus.SUCCESS)
            .visibility(ParticipationVisibility.PUBLIC)
            .likeCount(5L)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", participationId);

    RewardCatalog prizeCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN_COFFEE")
            .stock(0)
            .active(true)
            .build();
    UUID rewardId = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(prizeCatalog, "id", rewardId);

    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(participationRepository.findTopCandidates(any(), any(), any(), any()))
        .thenReturn(List.of(candidate));
    when(rewardCatalogRepository.findByCode("CPN_COFFEE")).thenReturn(Optional.of(prizeCatalog));
    when(rewardGrantRepository.existsByParticipationIdAndGrantReasonAndReward_Id(
            participationId, GrantReason.TOP_LIKE, rewardId))
        .thenReturn(false);

    TopLikeSettlementReportResDto report = settlementService.settleEvent(eventId);
    assertThat(report.totalSkippedStock()).isEqualTo(1);
    assertThat(report.items().get(0).status()).isEqualTo(SettlementItemStatus.SKIPPED_OUT_OF_STOCK);
  }

  @Test
  @DisplayName("실물 보상 코드가 없는 경우 포인트와 배지만 지급되고 상태는 GRANTED이다")
  void settleWithoutPrizeCode() {
    RewardSnapshot snapshot = new RewardSnapshot(100, null, 1, null);
    ZoneEvent event = ZoneEvent.builder().excellenceReward(snapshot).build();

    ZoneEventParticipation candidate =
        ZoneEventParticipation.builder()
            .userId(userId)
            .status(ParticipationStatus.SUCCESS)
            .visibility(ParticipationVisibility.PUBLIC)
            .likeCount(10L)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", participationId);

    when(zoneEventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(participationRepository.findTopCandidates(any(), any(), any(), any()))
        .thenReturn(List.of(candidate));

    RewardCatalog pointCatalog =
        RewardCatalog.builder().rewardType(RewardType.POINT).code("POINT_BASE").build();
    org.springframework.test.util.ReflectionTestUtils.setField(
        pointCatalog, "id", UUID.randomUUID());
    when(rewardCatalogRepository.findByCode("POINT_BASE")).thenReturn(Optional.of(pointCatalog));
    when(rewardGrantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TopLikeSettlementReportResDto report = settlementService.settleEvent(eventId);
    assertThat(report.totalGranted()).isEqualTo(1);
    assertThat(report.items().get(0).status()).isEqualTo(SettlementItemStatus.GRANTED);
  }

  @Test
  @DisplayName("settleRound는 회차 내 모든 이벤트에 대해 정산을 수행한다")
  void settleRoundSuccess() {
    ZoneEvent event1 =
        ZoneEvent.builder().excellenceReward(new RewardSnapshot(null, null, 0, null)).build();
    org.springframework.test.util.ReflectionTestUtils.setField(event1, "id", UUID.randomUUID());

    when(zoneEventRepository.findByRoundId(roundId)).thenReturn(List.of(event1));
    when(zoneEventRepository.findById(event1.getId())).thenReturn(Optional.of(event1));

    List<TopLikeSettlementReportResDto> reports = settlementService.settleRound(roundId);
    assertThat(reports).hasSize(1);
  }
}

package com.butingbe.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.butingbe.domain.reward.dto.response.ParticipationRevokeResDto;
import com.butingbe.domain.reward.entity.CouponStatus;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.entity.UserCoupon;
import com.butingbe.domain.reward.entity.UserPointLedger;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.reward.repository.UserBadgeRepository;
import com.butingbe.domain.reward.repository.UserCouponRepository;
import com.butingbe.domain.reward.repository.UserPointLedgerRepository;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RewardRevokeServiceTest {

  @Mock private ZoneEventParticipationRepository participationRepository;
  @Mock private RewardGrantRepository rewardGrantRepository;
  @Mock private UserCouponRepository userCouponRepository;
  @Mock private UserBadgeRepository userBadgeRepository;
  @Mock private UserPointService userPointService;
  @Mock private UserPointLedgerRepository userPointLedgerRepository;

  @InjectMocks private RewardRevokeService rewardRevokeService;

  private final UUID participationId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @Test
  @DisplayName("참여가 존재하지 않으면 ResourceNotFoundException이 발생한다")
  void participationNotFound() {
    when(participationRepository.findById(participationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> rewardRevokeService.revokeParticipation(participationId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("error.zone_event_participation.not_found");
  }

  @Test
  @DisplayName("참여가 무효화되면 포인트 원장 차감, 배지 삭제, 미사용 쿠폰 삭제 및 재고 복구가 수행된다")
  void revokeParticipationSuccess() {
    ZoneEventParticipation participation =
        ZoneEventParticipation.builder().userId(userId).status(ParticipationStatus.SUCCESS).build();
    when(participationRepository.findById(participationId)).thenReturn(Optional.of(participation));

    RewardCatalog pointCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.POINT)
            .code("POINT_BASE")
            .pointAmount(50)
            .build();
    UUID pointGrantId = UUID.randomUUID();
    RewardGrant pointGrant =
        RewardGrant.builder()
            .userId(userId)
            .reward(pointCatalog)
            .participationId(participationId)
            .grantReason(GrantReason.BASE)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(pointGrant, "id", pointGrantId);

    UserPointLedger ledger =
        UserPointLedger.builder()
            .userId(userId)
            .amount(100)
            .reason("BASE")
            .grantId(pointGrantId)
            .build();
    when(userPointLedgerRepository.findByGrantId(pointGrantId)).thenReturn(Optional.of(ledger));

    RewardCatalog badgeCatalog =
        RewardCatalog.builder().rewardType(RewardType.BADGE).code("BADGE_1").build();
    UUID badgeGrantId = UUID.randomUUID();
    RewardGrant badgeGrant =
        RewardGrant.builder()
            .userId(userId)
            .reward(badgeCatalog)
            .participationId(participationId)
            .grantReason(GrantReason.TOP_LIKE)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(badgeGrant, "id", badgeGrantId);

    RewardCatalog couponCatalog =
        RewardCatalog.builder().rewardType(RewardType.COUPON).code("CPN_COFFEE").stock(5).build();
    UUID couponGrantId = UUID.randomUUID();
    RewardGrant couponGrant =
        RewardGrant.builder()
            .userId(userId)
            .reward(couponCatalog)
            .participationId(participationId)
            .grantReason(GrantReason.TOP_LIKE)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(couponGrant, "id", couponGrantId);

    UserCoupon unusedCoupon =
        UserCoupon.builder()
            .userId(userId)
            .reward(couponCatalog)
            .grantId(couponGrantId)
            .status(CouponStatus.ISSUED)
            .couponCode("CPN-1111")
            .build();
    when(userCouponRepository.findByGrantId(couponGrantId)).thenReturn(Optional.of(unusedCoupon));

    when(rewardGrantRepository.findByParticipationIdAndRevokedAtIsNull(participationId))
        .thenReturn(List.of(pointGrant, badgeGrant, couponGrant));

    ParticipationRevokeResDto result = rewardRevokeService.revokeParticipation(participationId);

    assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.REVOKED);
    assertThat(result.revokedGrantsCount()).isEqualTo(3);
    assertThat(result.revokedPointsAmount()).isEqualTo(100);
    assertThat(result.revokedCouponsCount()).isEqualTo(1);
    assertThat(couponCatalog.getStock()).isEqualTo(6);

    verify(userPointService).record(eq(userId), eq(-100), eq("REVOKE"), eq(pointGrantId));
    verify(userBadgeRepository).deleteByGrantId(badgeGrantId);
    verify(userCouponRepository).delete(unusedCoupon);
  }

  @Test
  @DisplayName("이미 사용된 쿠폰은 회수되지 않고 보존된다")
  void usedCouponNotRevoked() {
    ZoneEventParticipation participation =
        ZoneEventParticipation.builder().userId(userId).status(ParticipationStatus.SUCCESS).build();
    when(participationRepository.findById(participationId)).thenReturn(Optional.of(participation));

    RewardCatalog couponCatalog =
        RewardCatalog.builder().rewardType(RewardType.COUPON).code("CPN_COFFEE").stock(5).build();
    UUID couponGrantId = UUID.randomUUID();
    RewardGrant couponGrant =
        RewardGrant.builder()
            .userId(userId)
            .reward(couponCatalog)
            .participationId(participationId)
            .grantReason(GrantReason.TOP_LIKE)
            .build();
    org.springframework.test.util.ReflectionTestUtils.setField(couponGrant, "id", couponGrantId);

    UserCoupon usedCoupon =
        UserCoupon.builder()
            .userId(userId)
            .reward(couponCatalog)
            .grantId(couponGrantId)
            .status(CouponStatus.USED)
            .couponCode("CPN-1111")
            .build();
    when(userCouponRepository.findByGrantId(couponGrantId)).thenReturn(Optional.of(usedCoupon));

    when(rewardGrantRepository.findByParticipationIdAndRevokedAtIsNull(participationId))
        .thenReturn(List.of(couponGrant));

    ParticipationRevokeResDto result = rewardRevokeService.revokeParticipation(participationId);

    assertThat(result.revokedCouponsCount()).isEqualTo(0);
    assertThat(result.revokedGrantsCount()).isEqualTo(0);
    assertThat(couponCatalog.getStock()).isEqualTo(5);
    assertThat(couponGrant.getRevokedAt()).isNull();
    verify(userCouponRepository, never()).delete(any());
  }

  @Test
  @DisplayName("BASE나 TOP_LIKE가 아닌 사유의 지급은 회수 대상에서 제외된다")
  void otherReasonNotRevoked() {
    ZoneEventParticipation participation =
        ZoneEventParticipation.builder().userId(userId).status(ParticipationStatus.SUCCESS).build();
    when(participationRepository.findById(participationId)).thenReturn(Optional.of(participation));

    RewardCatalog catalog =
        RewardCatalog.builder().rewardType(RewardType.POINT).code("OTHER").build();
    RewardGrant otherGrant =
        RewardGrant.builder()
            .userId(userId)
            .reward(catalog)
            .participationId(participationId)
            .grantReason(GrantReason.ZONE_WIN)
            .build();

    when(rewardGrantRepository.findByParticipationIdAndRevokedAtIsNull(participationId))
        .thenReturn(List.of(otherGrant));

    ParticipationRevokeResDto result = rewardRevokeService.revokeParticipation(participationId);
    assertThat(result.revokedGrantsCount()).isEqualTo(0);
    verify(userPointService, never()).record(any(), anyInt(), any(), any());
  }
}

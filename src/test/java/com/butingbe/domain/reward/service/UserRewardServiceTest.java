package com.butingbe.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.dto.response.PointLedgerPageResDto;
import com.butingbe.domain.reward.dto.response.UserRewardsResDto;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.entity.UserBadge;
import com.butingbe.domain.reward.entity.UserPointLedger;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.domain.reward.repository.UserBadgeRepository;
import com.butingbe.domain.reward.repository.UserPointLedgerRepository;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.repository.ZoneEventRepository;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserRewardServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private UserPointService userPointService;
  @Mock private UserPointLedgerRepository userPointLedgerRepository;
  @Mock private UserBadgeRepository userBadgeRepository;
  @Mock private RewardGrantRepository rewardGrantRepository;
  @Mock private ZoneEventRepository zoneEventRepository;
  @Mock private FileStorageService fileStorageService;

  private UserRewardService service;
  private AuthenticatedUser user;

  @BeforeEach
  void setUp() {
    service =
        new UserRewardService(
            userPointService,
            userPointLedgerRepository,
            userBadgeRepository,
            rewardGrantRepository,
            zoneEventRepository,
            fileStorageService);
    user = new AuthenticatedUser(USER_ID, "test@example.com", "tester", List.of());
  }

  @Test
  @DisplayName("미인증 유저는 보상 조회가 불가능하다")
  void getMyRewardsUnauthenticated() {
    assertThatThrownBy(() -> service.getMyRewards(null))
        .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  @DisplayName("유저의 포인트, 6대 구역 배지, 쿠폰 목록을 정상 조회한다")
  void getMyRewardsSuccess() {
    when(userPointService.getBalance(USER_ID)).thenReturn(150);

    UUID grantId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    RewardCatalog badgeCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.BADGE)
            .code("SPOT_GWANGAN_BRIDGE")
            .name("광안대교 배지")
            .imageFileKey("badges/gwangan.png")
            .build();
    ReflectionTestUtils.setField(badgeCatalog, "id", UUID.randomUUID());

    UserBadge badge =
        UserBadge.builder().userId(USER_ID).reward(badgeCatalog).grantId(grantId).build();
    ReflectionTestUtils.setField(badge, "id", UUID.randomUUID());

    when(userBadgeRepository.findByUserIdOrderByEarnedAtDesc(USER_ID)).thenReturn(List.of(badge));

    RewardGrant badgeGrant =
        RewardGrant.builder()
            .userId(USER_ID)
            .reward(badgeCatalog)
            .eventId(eventId)
            .grantReason(GrantReason.BASE)
            .build();
    ReflectionTestUtils.setField(badgeGrant, "id", grantId);

    when(rewardGrantRepository.findAllById(any())).thenReturn(List.of(badgeGrant));

    ZoneEvent event =
        ZoneEvent.builder()
            .zoneId("SUYEONG_NAMGU")
            .title("광안리 행사")
            .startsAt(OffsetDateTime.now())
            .durationMinutes(60)
            .build();
    ReflectionTestUtils.setField(event, "id", eventId);

    when(zoneEventRepository.findAllById(any())).thenReturn(List.of(event));
    when(fileStorageService.getPresignedUrl("badges/gwangan.png"))
        .thenReturn("https://storage/badges/gwangan.png");

    RewardCatalog couponCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN_COFFEE")
            .name("아메리카노 쿠폰")
            .validDays(30)
            .build();
    ReflectionTestUtils.setField(couponCatalog, "id", UUID.randomUUID());

    RewardGrant couponGrant =
        RewardGrant.builder()
            .userId(USER_ID)
            .reward(couponCatalog)
            .grantReason(GrantReason.BASE)
            .grantedAt(OffsetDateTime.now())
            .build();
    ReflectionTestUtils.setField(couponGrant, "id", UUID.randomUUID());

    when(rewardGrantRepository.findByUserIdOrderByGrantedAtDesc(USER_ID))
        .thenReturn(List.of(couponGrant));

    UserRewardsResDto response = service.getMyRewards(user);

    assertThat(response.pointBalance()).isEqualTo(150L);
    assertThat(response.badges()).hasSize(6);
    assertThat(response.coupons()).hasSize(1);
    assertThat(response.coupons().get(0).code()).isEqualTo("CPN_COFFEE");
  }

  @Test
  @DisplayName("포인트 원장 내역을 커서 페이징으로 조회한다")
  void getPointLedgerSuccess() {
    UserPointLedger ledger =
        UserPointLedger.builder().userId(USER_ID).amount(50).reason("BASE").build();
    ReflectionTestUtils.setField(ledger, "id", UUID.randomUUID());

    Page<UserPointLedger> page = new PageImpl<>(List.of(ledger));
    when(userPointLedgerRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenReturn(page);

    PointLedgerPageResDto response = service.getPointLedger(user, null, 10);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).amount()).isEqualTo(50);
    assertThat(response.hasNext()).isFalse();
  }

  @Test
  @DisplayName("포인트 원장 내역을 커서와 함께 조회하여 다음 커서를 반환한다")
  void getPointLedgerWithCursorAndHasNext() {
    UserPointLedger l1 =
        UserPointLedger.builder().userId(USER_ID).amount(50).reason("BASE").build();
    ReflectionTestUtils.setField(l1, "id", UUID.randomUUID());

    UserPointLedger l2 =
        UserPointLedger.builder().userId(USER_ID).amount(100).reason("EXCELLENCE").build();
    ReflectionTestUtils.setField(l2, "id", UUID.randomUUID());

    Page<UserPointLedger> page = new PageImpl<>(List.of(l1, l2));
    when(userPointLedgerRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenAnswer(
            invocation -> {
              Specification<UserPointLedger> spec = invocation.getArgument(0);
              if (spec != null) {
                jakarta.persistence.criteria.Root<UserPointLedger> root =
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

    String cursor =
        java.util.Base64.getUrlEncoder()
            .encodeToString("2026-09-01T00:00:00Z_00000000-0000-0000-0000-000000000001".getBytes());

    PointLedgerPageResDto response = service.getPointLedger(user, cursor, 1);

    assertThat(response.items()).hasSize(1);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("포인트 원장 조회 시 잘못된 커서이면 IllegalArgumentException을 던진다")
  void getPointLedgerInvalidCursor() {
    assertThatThrownBy(() -> service.getPointLedger(user, "invalid_cursor", 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("만료된 쿠폰은 유효기간이 과거로 조회된다")
  void getMyRewardsExpiredCoupon() {
    when(userPointService.getBalance(USER_ID)).thenReturn(0);
    when(userBadgeRepository.findByUserIdOrderByEarnedAtDesc(USER_ID)).thenReturn(List.of());

    RewardCatalog couponCatalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN_EXPIRED")
            .name("만료된 쿠폰")
            .validDays(1)
            .build();
    ReflectionTestUtils.setField(couponCatalog, "id", UUID.randomUUID());

    RewardGrant couponGrant =
        RewardGrant.builder()
            .userId(USER_ID)
            .reward(couponCatalog)
            .grantReason(GrantReason.BASE)
            .grantedAt(OffsetDateTime.now().minusDays(5))
            .build();
    ReflectionTestUtils.setField(couponGrant, "id", UUID.randomUUID());

    when(rewardGrantRepository.findByUserIdOrderByGrantedAtDesc(USER_ID))
        .thenReturn(List.of(couponGrant));

    UserRewardsResDto response = service.getMyRewards(user);
    assertThat(response.coupons()).hasSize(1);
    assertThat(response.coupons().get(0).validUntil()).isBefore(OffsetDateTime.now());
  }
}

package com.butingbe.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.reward.dto.request.AdminPayoutReleaseHoldReqDto;
import com.butingbe.domain.reward.dto.response.AdminPayoutItemResDto;
import com.butingbe.domain.reward.entity.BaseRewardPayout;
import com.butingbe.domain.reward.entity.BaseRewardPayoutStatus;
import com.butingbe.domain.reward.entity.PayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import com.butingbe.domain.reward.repository.BaseRewardPayoutRepository;
import com.butingbe.domain.reward.repository.RewardPayoutRepository;
import com.butingbe.domain.user.entity.Name;
import com.butingbe.domain.user.entity.User;
import com.butingbe.domain.user.entity.UserRole;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.entity.ReportReasonCode;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.RewardSnapshot;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.support.AbstractContainerTest;
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
class RewardPayoutHoldServiceTest extends AbstractContainerTest {

  @Autowired private RewardPayoutHoldService rewardPayoutHoldService;
  @Autowired private BaseRewardPayoutRepository baseRewardPayoutRepository;
  @Autowired private RewardPayoutRepository rewardPayoutRepository;
  @Autowired private ZoneEventReportRepository reportRepository;
  @Autowired private UserRepository userRepository;

  private AuthenticatedUser operator;
  private AuthenticatedUser normalUser;

  @BeforeEach
  void setUp() {
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
  @DisplayName("미지급 상태의 기본 보상과 우수 보상은 신고 발생 시 HELD_REPORT로 전환된다")
  void holdUnpaidPayoutsTransitionsToHeld() {
    UUID participationId = UUID.randomUUID();
    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(participationId)
                .reward(new RewardSnapshot(100, null, null, null))
                .build());

    RewardPayout rewardPayout =
        rewardPayoutRepository.save(
            RewardPayout.builder()
                .eventId(UUID.randomUUID())
                .participationId(participationId)
                .rankN(1)
                .likeCountAtClose(10L)
                .reward(new RewardSnapshot(500, null, null, null))
                .build());

    rewardPayoutHoldService.holdUnpaidPayouts(participationId);

    BaseRewardPayout updatedBase =
        baseRewardPayoutRepository.findById(basePayout.getId()).orElseThrow();
    RewardPayout updatedReward =
        rewardPayoutRepository.findById(rewardPayout.getId()).orElseThrow();

    assertThat(updatedBase.getHoldStatus()).isEqualTo(PayoutHoldStatus.HELD_REPORT);
    assertThat(updatedBase.getStatus()).isEqualTo(BaseRewardPayoutStatus.PENDING_CONFIRM);
    assertThat(updatedReward.getHoldStatus()).isEqualTo(PayoutHoldStatus.HELD_REPORT);
    assertThat(updatedReward.getStatus()).isEqualTo(RewardPayoutStatus.PENDING_ASSIGN);
  }

  @Test
  @DisplayName("이미 지급 완료(PAID, SENT)된 보상은 보류되지 않는다")
  void alreadyPaidPayoutsAreNotHeld() {
    UUID participationId = UUID.randomUUID();
    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(participationId)
                .reward(new RewardSnapshot(100, null, null, null))
                .build());
    ReflectionTestUtils.setField(basePayout, "status", BaseRewardPayoutStatus.PAID);
    baseRewardPayoutRepository.save(basePayout);

    RewardPayout rewardPayout =
        rewardPayoutRepository.save(
            RewardPayout.builder()
                .eventId(UUID.randomUUID())
                .participationId(participationId)
                .rankN(1)
                .likeCountAtClose(10L)
                .reward(new RewardSnapshot(500, null, null, null))
                .build());
    ReflectionTestUtils.setField(rewardPayout, "status", RewardPayoutStatus.SENT);
    rewardPayoutRepository.save(rewardPayout);

    rewardPayoutHoldService.holdUnpaidPayouts(participationId);

    BaseRewardPayout updatedBase =
        baseRewardPayoutRepository.findById(basePayout.getId()).orElseThrow();
    RewardPayout updatedReward =
        rewardPayoutRepository.findById(rewardPayout.getId()).orElseThrow();

    assertThat(updatedBase.getHoldStatus()).isEqualTo(PayoutHoldStatus.NONE);
    assertThat(updatedReward.getHoldStatus()).isEqualTo(PayoutHoldStatus.NONE);
  }

  @Test
  @DisplayName("미해결 신고(OPEN, REVIEWING)가 존재하면 보류 해제 시 409 예외가 발생한다")
  void releaseHoldFailsWhenUnresolvedReportsExist() {
    UUID participationId = UUID.randomUUID();
    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(participationId)
                .reward(new RewardSnapshot(100, null, null, null))
                .build());
    basePayout.hold();
    baseRewardPayoutRepository.save(basePayout);

    reportRepository.save(
        ZoneEventReport.builder()
            .participationId(participationId)
            .reporterId(UUID.randomUUID())
            .reasonCode(ReportReasonCode.NOT_ON_SITE)
            .memo("미해결 신고")
            .build());

    AdminPayoutReleaseHoldReqDto req =
        new AdminPayoutReleaseHoldReqDto("검수 완료", basePayout.getRevision());

    assertThatThrownBy(() -> rewardPayoutHoldService.releaseHold(operator, basePayout.getId(), req))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.reward.payout.unresolved_reports");
  }

  @Test
  @DisplayName("모든 신고가 해결(UPHELD 또는 DISMISSED)되었거나 없으면 보류가 정상 해제되고 status는 유지된다")
  void releaseHoldSucceedsWhenReportsAreResolved() {
    UUID participationId = UUID.randomUUID();
    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(participationId)
                .reward(new RewardSnapshot(100, null, null, null))
                .build());
    basePayout.hold();
    baseRewardPayoutRepository.save(basePayout);

    ZoneEventReport report =
        reportRepository.save(
            ZoneEventReport.builder()
                .participationId(participationId)
                .reporterId(UUID.randomUUID())
                .reasonCode(ReportReasonCode.NOT_ON_SITE)
                .memo("처리된 신고")
                .build());
    report.resolveAs(ReportStatus.DISMISSED);

    AdminPayoutReleaseHoldReqDto req =
        new AdminPayoutReleaseHoldReqDto("정상 확인", basePayout.getRevision());
    AdminPayoutItemResDto result =
        rewardPayoutHoldService.releaseHold(operator, basePayout.getId(), req);

    assertThat(result.holdStatus()).isEqualTo(PayoutHoldStatus.NONE);
    assertThat(result.status()).isEqualTo("PENDING_CONFIRM");

    BaseRewardPayout reloaded =
        baseRewardPayoutRepository.findById(basePayout.getId()).orElseThrow();
    assertThat(reloaded.getHoldStatus()).isEqualTo(PayoutHoldStatus.NONE);
    assertThat(reloaded.getStatus()).isEqualTo(BaseRewardPayoutStatus.PENDING_CONFIRM);
  }

  @Test
  @DisplayName("리비전이 불일치하면 409 충돌 예외가 발생한다")
  void revisionMismatchThrowsConflict() {
    UUID participationId = UUID.randomUUID();
    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(participationId)
                .reward(new RewardSnapshot(100, null, null, null))
                .build());
    basePayout.hold();
    baseRewardPayoutRepository.save(basePayout);

    AdminPayoutReleaseHoldReqDto req = new AdminPayoutReleaseHoldReqDto("메모", 9999L);

    assertThatThrownBy(() -> rewardPayoutHoldService.releaseHold(operator, basePayout.getId(), req))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.revision.conflict");
  }

  @Test
  @DisplayName("일반 사용자가 보류 해제를 시도하면 403 예외가 발생한다")
  void nonOperatorThrowsForbidden() {
    UUID participationId = UUID.randomUUID();
    BaseRewardPayout basePayout =
        baseRewardPayoutRepository.save(
            BaseRewardPayout.builder()
                .participationId(participationId)
                .reward(new RewardSnapshot(100, null, null, null))
                .build());
    basePayout.hold();
    baseRewardPayoutRepository.save(basePayout);

    AdminPayoutReleaseHoldReqDto req =
        new AdminPayoutReleaseHoldReqDto("메모", basePayout.getRevision());

    assertThatThrownBy(
            () -> rewardPayoutHoldService.releaseHold(normalUser, basePayout.getId(), req))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("존재하지 않는 지급 건에 대해 보류 해제를 시도하면 404 예외가 발생한다")
  void nonExistentPayoutThrowsNotFound() {
    AdminPayoutReleaseHoldReqDto req = new AdminPayoutReleaseHoldReqDto("메모", 0L);

    assertThatThrownBy(() -> rewardPayoutHoldService.releaseHold(operator, UUID.randomUUID(), req))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}

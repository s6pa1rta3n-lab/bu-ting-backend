package com.butingbe.domain.reward.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RewardPayout} domain entity. */
class RewardPayoutTest {

  @Test
  @DisplayName("RewardPayout 빌더 기본값 및 전체 라이프사이클 메서드가 정상 동작한다")
  void rewardPayoutFullLifecycleTest() {
    UUID roundId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID participationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();

    RewardPayout payout =
        RewardPayout.builder()
            .roundId(roundId)
            .eventId(eventId)
            .participationId(participationId)
            .userId(userId)
            .rewardReason(GrantReason.TOP_LIKE)
            .rankN(1)
            .likeCountAtClose(25)
            .status(RewardPayoutStatus.PENDING_ASSIGN)
            .holdStatus(null)
            .revision(null)
            .build();

    assertThat(payout.getRoundId()).isEqualTo(roundId);
    assertThat(payout.getEventId()).isEqualTo(eventId);
    assertThat(payout.getParticipationId()).isEqualTo(participationId);
    assertThat(payout.getUserId()).isEqualTo(userId);
    assertThat(payout.getRewardReason()).isEqualTo(GrantReason.TOP_LIKE);
    assertThat(payout.getRankN()).isEqualTo(1);
    assertThat(payout.getLikeCountAtClose()).isEqualTo(25);
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.PENDING_ASSIGN);
    assertThat(payout.getHoldStatus()).isEqualTo(RewardPayoutHoldStatus.NONE);
    assertThat(payout.getRevision()).isEqualTo(0L);
    assertThat(payout.getRetryCount()).isEqualTo(0);
    assertThat(payout.getHistories()).isEmpty();

    payout.addHistory(
        RewardPayoutAction.CONFIRM,
        RewardPayoutStatus.PENDING_ASSIGN,
        RewardPayoutStatus.PENDING_CONFIRM,
        actorId,
        "상태 변경");
    assertThat(payout.getHistories()).hasSize(1);

    payout.incrementRevision();
    assertThat(payout.getRevision()).isEqualTo(1L);

    OffsetDateTime targetSchedule = OffsetDateTime.now().plusDays(3);
    payout.schedule(targetSchedule);
    assertThat(payout.getScheduledAt()).isEqualTo(targetSchedule);

    payout.updateRewardConfig(null, 100, "BADGE-1", "선물", targetSchedule.plusDays(1), "설정 변경");
    assertThat(payout.getPoints()).isEqualTo(100);
    assertThat(payout.getBadgeCode()).isEqualTo("BADGE-1");
    assertThat(payout.getPrizeName()).isEqualTo("선물");
    assertThat(payout.getScheduledAt()).isEqualTo(targetSchedule.plusDays(1));
    assertThat(payout.getNote()).isEqualTo("설정 변경");
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.PENDING_CONFIRM);

    payout.hold(null);
    assertThat(payout.getHoldStatus()).isEqualTo(RewardPayoutHoldStatus.HELD_REPORT);

    payout.hold("부정수급 신고");
    assertThat(payout.getNote()).isEqualTo("부정수급 신고");

    payout.releaseHold(null);
    assertThat(payout.getHoldStatus()).isEqualTo(RewardPayoutHoldStatus.NONE);

    payout.releaseHold("소명 완료");
    assertThat(payout.getNote()).isEqualTo("소명 완료");

    payout.confirm(actorId);
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.CONFIRMED);
    assertThat(payout.getConfirmedBy()).isEqualTo(actorId);
    assertThat(payout.getConfirmedAt()).isNotNull();

    payout.markMailSent(null, null);
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.MAIL_SENT);
    assertThat(payout.getMailedAt()).isNotNull();

    payout.markMailSent(OffsetDateTime.now(), "메일 발송 완료");
    assertThat(payout.getNote()).isEqualTo("메일 발송 완료");

    payout.markInfoCollected(null, null);
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.INFO_COLLECTED);
    assertThat(payout.getInformationCollectedAt()).isNotNull();

    payout.markInfoCollected(OffsetDateTime.now(), "정보 수집 완료");
    assertThat(payout.getNote()).isEqualTo("정보 수집 완료");

    payout.markSent(null, null, null);
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.SENT);
    assertThat(payout.getSentAt()).isNotNull();

    payout.markSent(OffsetDateTime.now(), "WAYBILL-1234", "발송 처리");
    assertThat(payout.getReference()).isEqualTo("WAYBILL-1234");
    assertThat(payout.getNote()).isEqualTo("발송 처리");

    payout.markPaid(null);
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.PAID);
    assertThat(payout.getPaidAt()).isNotNull();

    payout.markPaid(OffsetDateTime.now());
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.PAID);

    payout.markFailed("LEDGER_FAILURE");
    assertThat(payout.getStatus()).isEqualTo(RewardPayoutStatus.FAILED);
    assertThat(payout.getFailureCode()).isEqualTo("LEDGER_FAILURE");

    payout.incrementRetryCount();
    assertThat(payout.getRetryCount()).isEqualTo(1);
  }
}

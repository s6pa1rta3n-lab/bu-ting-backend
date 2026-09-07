package com.butingbe.domain.reward.service;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.auth.security.OperatorAuthorization;
import com.butingbe.domain.reward.dto.request.AdminPayoutReleaseHoldReqDto;
import com.butingbe.domain.reward.dto.response.AdminPayoutItemResDto;
import com.butingbe.domain.reward.entity.BaseRewardPayout;
import com.butingbe.domain.reward.entity.BaseRewardPayoutStatus;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.entity.RewardPayoutStatus;
import com.butingbe.domain.reward.repository.BaseRewardPayoutRepository;
import com.butingbe.domain.reward.repository.RewardPayoutRepository;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 신고 연동 보상 지급 보류 및 해제 처리 서비스. */
@Service
@RequiredArgsConstructor
public class RewardPayoutHoldService {

  private final BaseRewardPayoutRepository baseRewardPayoutRepository;
  private final RewardPayoutRepository rewardPayoutRepository;
  private final ZoneEventReportRepository reportRepository;
  private final OperatorAuthorization operatorAuthorization;

  /**
   * 참여에 연결된 미지급 상태의 기본 보상 및 우수 보상을 모두 보류(HELD_REPORT)로 전환한다.
   *
   * @param participationId 참여 식별자
   */
  @Transactional
  public void holdUnpaidPayouts(UUID participationId) {
    baseRewardPayoutRepository
        .findByParticipationId(participationId)
        .filter(payout -> payout.getStatus() != BaseRewardPayoutStatus.PAID)
        .ifPresent(BaseRewardPayout::hold);

    rewardPayoutRepository
        .findByParticipationId(participationId)
        .filter(payout -> payout.getStatus() != RewardPayoutStatus.SENT)
        .ifPresent(RewardPayout::hold);
  }

  /**
   * 관리자가 특정 지급 건의 보류를 해제한다. 미해결 신고가 남아있으면 해제할 수 없다.
   *
   * @param user 요청 관리자
   * @param payoutId 지급 건 식별자
   * @param req 해제 요청 본문
   * @return 보류가 해제된 지급 건 정보
   */
  @Transactional
  public AdminPayoutItemResDto releaseHold(
      AuthenticatedUser user, UUID payoutId, AdminPayoutReleaseHoldReqDto req) {
    operatorAuthorization.requireOperator(user);

    Optional<RewardPayout> rewardPayoutOpt = rewardPayoutRepository.findById(payoutId);
    if (rewardPayoutOpt.isPresent()) {
      RewardPayout payout = rewardPayoutOpt.get();
      validateRevision(req.expectedRevision(), payout.getRevision());
      checkNoUnresolvedReports(payout.getParticipationId());
      payout.releaseHold();
      return AdminPayoutItemResDto.from(payout);
    }

    Optional<BaseRewardPayout> basePayoutOpt = baseRewardPayoutRepository.findById(payoutId);
    if (basePayoutOpt.isPresent()) {
      BaseRewardPayout payout = basePayoutOpt.get();
      validateRevision(req.expectedRevision(), payout.getRevision());
      checkNoUnresolvedReports(payout.getParticipationId());
      payout.releaseHold();
      return AdminPayoutItemResDto.from(payout);
    }

    throw new ResourceNotFoundException("error.reward.payout.not_found");
  }

  private void validateRevision(Long expectedRevision, Long currentRevision) {
    if (expectedRevision != null && !expectedRevision.equals(currentRevision)) {
      throw new ConflictException("error.revision.conflict");
    }
  }

  private void checkNoUnresolvedReports(UUID participationId) {
    boolean hasUnresolved =
        reportRepository.existsByParticipationIdAndStatusIn(
            participationId, List.of(ReportStatus.OPEN, ReportStatus.REVIEWING));
    if (hasUnresolved) {
      throw new ConflictException("error.reward.payout.unresolved_reports");
    }
  }
}

package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.auth.security.OperatorAuthorization;
import com.butingbe.domain.reward.dto.response.AdminPayoutItemResDto;
import com.butingbe.domain.reward.entity.BaseRewardPayout;
import com.butingbe.domain.reward.entity.PayoutHoldStatus;
import com.butingbe.domain.reward.entity.RewardPayout;
import com.butingbe.domain.reward.repository.BaseRewardPayoutRepository;
import com.butingbe.domain.reward.repository.RewardPayoutRepository;
import com.butingbe.domain.reward.service.RewardPayoutHoldService;
import com.butingbe.domain.zoneevent.dto.request.AdminReportDismissReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminReportUpholdReqDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportDetailResDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportPageResDto;
import com.butingbe.domain.zoneevent.dto.response.AdminReportSummaryResDto;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 신고 관리자 검수 처리 및 조회 서비스. */
@Service
@RequiredArgsConstructor
public class AdminReportService {

  private final ZoneEventReportRepository reportRepository;
  private final ZoneEventParticipationRepository participationRepository;
  private final BaseRewardPayoutRepository baseRewardPayoutRepository;
  private final RewardPayoutRepository rewardPayoutRepository;
  private final RewardPayoutHoldService rewardPayoutHoldService;
  private final OperatorAuthorization operatorAuthorization;

  /**
   * 필터 조건과 페이징에 따라 신고 목록을 조회한다.
   *
   * @param user 요청 관리자
   * @param status 신고 상태 필터
   * @param roundId 회차 식별자 필터
   * @param eventId 이벤트 식별자 필터
   * @param participationId 참여 식별자 필터
   * @param pageable 페이징 정보
   * @return 신고 목록 페이징 응답
   */
  @Transactional(readOnly = true)
  public AdminReportPageResDto getReports(
      AuthenticatedUser user,
      ReportStatus status,
      UUID roundId,
      UUID eventId,
      UUID participationId,
      Pageable pageable) {
    operatorAuthorization.requireOperator(user);

    Page<ZoneEventReport> page =
        reportRepository.searchReports(status, roundId, eventId, participationId, pageable);

    List<ZoneEventReport> reports = page.getContent();
    if (reports.isEmpty()) {
      return new AdminReportPageResDto(
          List.of(),
          page.getNumber(),
          page.getSize(),
          page.getTotalElements(),
          page.getTotalPages());
    }

    Set<UUID> participationIds =
        reports.stream().map(ZoneEventReport::getParticipationId).collect(Collectors.toSet());

    Map<UUID, ZoneEventParticipation> participationMap =
        participationRepository.findAllById(participationIds).stream()
            .collect(
                Collectors.toMap(ZoneEventParticipation::getId, Function.identity(), (a, b) -> a));

    Map<UUID, BaseRewardPayout> basePayoutMap =
        baseRewardPayoutRepository.findByParticipationIdIn(participationIds).stream()
            .collect(
                Collectors.toMap(
                    BaseRewardPayout::getParticipationId, Function.identity(), (a, b) -> a));

    Map<UUID, RewardPayout> rewardPayoutMap =
        rewardPayoutRepository.findByParticipationIdIn(participationIds).stream()
            .collect(
                Collectors.toMap(
                    RewardPayout::getParticipationId, Function.identity(), (a, b) -> a));

    List<AdminReportSummaryResDto> items =
        reports.stream()
            .map(
                report -> {
                  ZoneEventParticipation p = participationMap.get(report.getParticipationId());
                  UUID pEventId = p != null && p.getEvent() != null ? p.getEvent().getId() : null;
                  UUID pRoundId =
                      p != null && p.getEvent() != null ? p.getEvent().getRoundId() : null;
                  UUID userId = p != null ? p.getUserId() : null;

                  BaseRewardPayout bp = basePayoutMap.get(report.getParticipationId());
                  RewardPayout rp = rewardPayoutMap.get(report.getParticipationId());
                  boolean isHeld =
                      (bp != null && bp.getHoldStatus() == PayoutHoldStatus.HELD_REPORT)
                          || (rp != null && rp.getHoldStatus() == PayoutHoldStatus.HELD_REPORT);
                  PayoutHoldStatus holdStatus =
                      isHeld ? PayoutHoldStatus.HELD_REPORT : PayoutHoldStatus.NONE;

                  return new AdminReportSummaryResDto(
                      report.getId(),
                      report.getReporterId(),
                      report.getReasonCode(),
                      report.getMemo(),
                      report.getStatus(),
                      report.getCreatedAt(),
                      report.getReviewedBy(),
                      report.getReviewedAt(),
                      report.getDecisionNote(),
                      report.getRevision(),
                      report.getParticipationId(),
                      pEventId,
                      pRoundId,
                      userId,
                      holdStatus);
                })
            .toList();

    return new AdminReportPageResDto(
        items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  /**
   * 신고 상세 정보, 검수 대상 및 관련 지급 건을 함께 조회한다.
   *
   * @param user 요청 관리자
   * @param reportId 신고 식별자
   * @return 신고 상세 응답 DTO
   */
  @Transactional(readOnly = true)
  public AdminReportDetailResDto getReportDetail(AuthenticatedUser user, UUID reportId) {
    operatorAuthorization.requireOperator(user);

    ZoneEventReport report =
        reportRepository
            .findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.report.not_found"));

    return buildDetailResDto(report);
  }

  /**
   * 신고를 인정(UPHELD) 처리하고 관련 미지급 보상을 보류한다.
   *
   * @param user 요청 관리자
   * @param reportId 신고 식별자
   * @param req 인정 요청 DTO
   * @return 갱신된 신고 상세 응답 DTO
   */
  @Transactional
  public AdminReportDetailResDto upholdReport(
      AuthenticatedUser user, UUID reportId, AdminReportUpholdReqDto req) {
    operatorAuthorization.requireOperator(user);

    ZoneEventReport report =
        reportRepository
            .findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.report.not_found"));

    if (req.expectedRevision() != null && !req.expectedRevision().equals(report.getRevision())) {
      throw new ConflictException("error.revision.conflict");
    }

    report.resolveAs(ReportStatus.UPHELD);
    report.stampDecision(user.id(), req.note());

    rewardPayoutHoldService.holdUnpaidPayouts(report.getParticipationId());

    return buildDetailResDto(report);
  }

  /**
   * 신고를 기각(DISMISSED) 처리한다. 기존 지급 보류는 유지된다.
   *
   * @param user 요청 관리자
   * @param reportId 신고 식별자
   * @param req 기각 요청 DTO
   * @return 갱신된 신고 상세 응답 DTO
   */
  @Transactional
  public AdminReportDetailResDto dismissReport(
      AuthenticatedUser user, UUID reportId, AdminReportDismissReqDto req) {
    operatorAuthorization.requireOperator(user);

    ZoneEventReport report =
        reportRepository
            .findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("error.zone_event.report.not_found"));

    if (req.expectedRevision() != null && !req.expectedRevision().equals(report.getRevision())) {
      throw new ConflictException("error.revision.conflict");
    }

    report.resolveAs(ReportStatus.DISMISSED);
    report.stampDecision(user.id(), req.note());

    return buildDetailResDto(report);
  }

  private AdminReportDetailResDto buildDetailResDto(ZoneEventReport report) {
    ZoneEventParticipation participation =
        participationRepository.findById(report.getParticipationId()).orElse(null);

    List<AdminPayoutItemResDto> payouts = new ArrayList<>();
    baseRewardPayoutRepository
        .findByParticipationId(report.getParticipationId())
        .ifPresent(p -> payouts.add(AdminPayoutItemResDto.from(p)));
    rewardPayoutRepository
        .findByParticipationId(report.getParticipationId())
        .ifPresent(p -> payouts.add(AdminPayoutItemResDto.from(p)));

    return new AdminReportDetailResDto(
        AdminReportDetailResDto.ReportInfo.from(report),
        AdminReportDetailResDto.TargetInfo.from(participation),
        payouts);
  }
}

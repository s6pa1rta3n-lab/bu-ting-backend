package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.notification.service.NotificationService;
import com.butingbe.domain.reward.service.RewardRevokeService;
import com.butingbe.domain.reward.service.RewardService;
import com.butingbe.domain.user.entity.User;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.response.ReviewQueueItemResDto;
import com.butingbe.domain.zoneevent.dto.response.ReviewQueuePageResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuditLog;
import com.butingbe.domain.zoneevent.entity.ZoneEventAuthTarget;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuditLogRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventAuthTargetRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.domain.zoneevent.support.GpsDistance;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for administrative review, manual approval, rejection, and reward revocation. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminZoneEventReviewService {

  private final ZoneEventParticipationRepository participationRepository;
  private final ZoneEventAuthTargetRepository authTargetRepository;
  private final ZoneEventReportRepository reportRepository;
  private final ZoneEventAuditLogRepository auditLogRepository;
  private final UserRepository userRepository;
  private final RewardService rewardService;
  private final RewardRevokeService rewardRevokeService;
  private final ZoneTitleService zoneTitleService;
  private final NotificationService notificationService;
  private final FileStorageService fileStorageService;

  /** Queries items requiring operator inspection (UNDER_REVIEW or hidden). */
  @Transactional(readOnly = true)
  public ReviewQueuePageResDto getReviewQueue(int page, int size) {
    int pageSize = Math.min(Math.max(size, 1), 50);
    int pageNumber = Math.max(page, 0);

    Page<ZoneEventParticipation> result =
        participationRepository.findByStatusOrHiddenTrue(
            ParticipationStatus.UNDER_REVIEW,
            PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "joinedAt")));

    List<ReviewQueueItemResDto> items =
        result.getContent().stream().map(this::toReviewQueueItem).collect(Collectors.toList());

    return new ReviewQueuePageResDto(items, (int) result.getTotalElements(), result.hasNext());
  }

  /** Approves an under-review participation and distributes rewards and titles. */
  @Transactional
  public void approve(UUID reviewerId, UUID participationId) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    if (participation.getStatus() != ParticipationStatus.UNDER_REVIEW
        && !Boolean.TRUE.equals(participation.getHidden())) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    participation.approveReview(reviewerId);
    if (Boolean.TRUE.equals(participation.getHidden())) {
      participation.unhide();
    }

    List<ZoneEventReport> reports =
        reportRepository.findByParticipationIdAndStatus(participationId, ReportStatus.OPEN);
    reports.forEach(ZoneEventReport::resolve);

    com.butingbe.domain.zoneevent.entity.RewardSnapshot baseReward =
        participation.getEvent().getBaseReward();
    if (baseReward != null) {
      rewardService.grantBaseReward(
          participation.getUserId(),
          participation.getId(),
          participation.getEvent().getId(),
          baseReward.points(),
          baseReward.badgeCode());
    }

    zoneTitleService.evaluateAndAwardTitles(
        participation.getUserId(), participation.getEvent().getZoneId());

    auditLogRepository.save(
        ZoneEventAuditLog.builder()
            .operatorId(reviewerId)
            .action("REVIEW_APPROVE")
            .targetType("PARTICIPATION")
            .targetId(participationId.toString())
            .details("Approved review for participation")
            .build());

    notificationService.sendToUser(
        participation.getUserId(), "인증 검수 승인", "제출하신 인증이 관리자 검수를 통과하여 보상이 지급되었습니다.", "ZONE_EVENT");
  }

  /** Manually rejects a participation under review. */
  @Transactional
  public void reject(UUID reviewerId, UUID participationId, String failReason) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    if (participation.getStatus() != ParticipationStatus.UNDER_REVIEW) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    participation.rejectReview(failReason, reviewerId);

    auditLogRepository.save(
        ZoneEventAuditLog.builder()
            .operatorId(reviewerId)
            .action("REVIEW_REJECT")
            .targetType("PARTICIPATION")
            .targetId(participationId.toString())
            .details("Rejected review: " + failReason)
            .build());

    notificationService.sendToUser(
        participation.getUserId(),
        "인증 반려 안내",
        "제출하신 인증이 검수 기준을 충족하지 못해 반려되었습니다: " + failReason,
        "ZONE_EVENT");
  }

  /** Revokes a previously approved/successful participation. */
  @Transactional
  public void revoke(UUID reviewerId, UUID participationId, String reason) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    if (participation.getStatus() != ParticipationStatus.SUCCESS) {
      throw new ConflictException("error.zone_event.invalid_state");
    }

    participation.revoke(reviewerId);
    rewardRevokeService.revokeParticipation(participationId);

    auditLogRepository.save(
        ZoneEventAuditLog.builder()
            .operatorId(reviewerId)
            .action("REVIEW_REVOKE")
            .targetType("PARTICIPATION")
            .targetId(participationId.toString())
            .details("Revoked success participation: " + reason)
            .build());

    notificationService.sendToUser(
        participation.getUserId(), "인증 취소 안내", "이상 징후가 확인되어 이벤트 인증 및 보상이 회수되었습니다.", "ZONE_EVENT");
  }

  /** Unhides a hidden participation and dismisses open reports. */
  @Transactional
  public void unhide(UUID reviewerId, UUID participationId) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    participation.unhide();
    List<ZoneEventReport> reports =
        reportRepository.findByParticipationIdAndStatus(participationId, ReportStatus.OPEN);
    reports.forEach(ZoneEventReport::dismiss);

    auditLogRepository.save(
        ZoneEventAuditLog.builder()
            .operatorId(reviewerId)
            .action("REVIEW_UNHIDE")
            .targetType("PARTICIPATION")
            .targetId(participationId.toString())
            .details("Unhidden by operator")
            .build());
  }

  private ReviewQueueItemResDto toReviewQueueItem(ZoneEventParticipation p) {
    User user = userRepository.findById(p.getUserId()).orElse(null);
    String nickname = user == null ? "Unknown" : user.getNickname();

    ZoneEventAuthTarget target =
        authTargetRepository.findByEvent_Id(p.getEvent().getId()).orElse(null);

    Double targetLat = target == null ? null : target.getLatitude();
    Double targetLng = target == null ? null : target.getLongitude();
    Double submitLat = p.getSubmitGpsLat() != null ? p.getSubmitGpsLat() : p.getGpsLat();
    Double submitLng = p.getSubmitGpsLng() != null ? p.getSubmitGpsLng() : p.getGpsLng();
    Integer distance = null;

    if (target != null && submitLat != null && submitLng != null) {
      distance =
          GpsDistance.meters(submitLat, submitLng, target.getLatitude(), target.getLongitude());
    }

    List<String> reasons = new ArrayList<>();
    if (Boolean.TRUE.equals(p.getHidden())) {
      reasons.add("HIDDEN_BY_REPORTS");
    }

    long openReportCount =
        reportRepository.countByParticipationIdAndStatus(p.getId(), ReportStatus.OPEN);
    if (openReportCount > 0) {
      reasons.add("OPEN_REPORTS_" + openReportCount);
    }

    if (p.getCapturedAt() != null && p.getCompletedAt() != null) {
      long deltaMinutes =
          Math.abs(Duration.between(p.getCapturedAt(), p.getCompletedAt()).toMinutes());
      if (deltaMinutes > 15) {
        reasons.add("CAPTURE_TIME_DELTA_MINUTES_" + deltaMinutes);
      }
    }

    String mediaUrl = null;
    if (p.getMediaFileKey() != null) {
      try {
        mediaUrl = fileStorageService.getPresignedUrl(p.getMediaFileKey());
      } catch (Exception ignored) {
      }
    }

    return new ReviewQueueItemResDto(
        p.getId(),
        p.getEvent().getId(),
        p.getEvent().getTitle(),
        p.getEvent().getZoneId(),
        p.getUserId(),
        nickname,
        mediaUrl,
        p.getContent(),
        targetLat,
        targetLng,
        submitLat,
        submitLng,
        distance,
        p.getCapturedAt(),
        p.getCompletedAt(),
        p.getStatus(),
        Boolean.TRUE.equals(p.getHidden()),
        openReportCount,
        reasons);
  }
}

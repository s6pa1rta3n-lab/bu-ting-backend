package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.user.entity.User;
import com.butingbe.domain.user.entity.UserRole;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.response.CommentResDto;
import com.butingbe.domain.zoneevent.dto.response.EquippedTitleResDto;
import com.butingbe.domain.zoneevent.dto.response.ReportResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.entity.ReportStatus;
import com.butingbe.domain.zoneevent.entity.ZoneEventComment;
import com.butingbe.domain.zoneevent.entity.ZoneEventLike;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.entity.ZoneEventReport;
import com.butingbe.domain.zoneevent.repository.ZoneEventCommentRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventLikeRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventReportRepository;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service handling social interactions: likes, comments, reports, and visibility toggling. */
@Service
@RequiredArgsConstructor
public class ZoneEventSocialService {

  private final ZoneEventParticipationRepository participationRepository;
  private final ZoneEventLikeRepository likeRepository;
  private final ZoneEventCommentRepository commentRepository;
  private final ZoneEventReportRepository reportRepository;
  private final UserRepository userRepository;
  private final ZoneTitleService zoneTitleService;

  /** Likes an event participation. Authors cannot like their own participation. */
  @Transactional
  public void likeParticipation(UUID userId, UUID participationId) {
    ZoneEventParticipation participation = getPublicActiveParticipation(participationId);

    if (participation.getUserId().equals(userId)) {
      throw new IllegalArgumentException("error.zone_event.social.cannot_like_own");
    }

    if (likeRepository.findByParticipationIdAndUserId(participationId, userId).isPresent()) {
      return;
    }

    likeRepository.save(
        ZoneEventLike.builder().participation(participation).userId(userId).build());
    participation.incrementLikeCount();
  }

  /** Unlikes an event participation. */
  @Transactional
  public void unlikeParticipation(UUID userId, UUID participationId) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    likeRepository
        .findByParticipationIdAndUserId(participationId, userId)
        .ifPresent(
            like -> {
              likeRepository.delete(like);
              participation.decrementLikeCount();
            });
  }

  /** Adds a comment to a public event participation. */
  @Transactional
  public CommentResDto addComment(UUID userId, UUID participationId, String content) {
    ZoneEventParticipation participation = getPublicActiveParticipation(participationId);

    ZoneEventComment comment =
        commentRepository.save(
            ZoneEventComment.builder()
                .participation(participation)
                .userId(userId)
                .content(content)
                .build());

    participation.incrementCommentCount();
    return buildCommentDto(comment, userId);
  }

  /** Lists active comments on an event participation. */
  @Transactional(readOnly = true)
  public List<CommentResDto> getComments(UUID userId, UUID participationId) {
    return commentRepository
        .findByParticipationIdAndDeletedAtIsNullOrderByCreatedAtAsc(participationId)
        .stream()
        .map(comment -> buildCommentDto(comment, userId))
        .collect(Collectors.toList());
  }

  /** Edits an existing comment. */
  @Transactional
  public CommentResDto updateComment(
      UUID userId, UUID commentId, String content, UserRole userRole) {
    ZoneEventComment comment =
        commentRepository
            .findById(commentId)
            .filter(c -> !c.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));

    if (!comment.getUserId().equals(userId) && !isOperator(userRole)) {
      throw new ForbiddenException("error.zone_event.participation.forbidden");
    }

    comment.updateContent(content);
    return buildCommentDto(comment, userId);
  }

  /** Soft-deletes an existing comment. */
  @Transactional
  public void deleteComment(UUID userId, UUID commentId, UserRole userRole) {
    ZoneEventComment comment =
        commentRepository
            .findById(commentId)
            .filter(c -> !c.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found"));

    if (!comment.getUserId().equals(userId) && !isOperator(userRole)) {
      throw new ForbiddenException("error.zone_event.participation.forbidden");
    }

    comment.softDelete();
    comment.getParticipation().decrementCommentCount();
  }

  /** Submits an abuse/inappropriate content report for an event participation. */
  @Transactional
  public ReportResDto reportParticipation(
      UUID reporterId, UUID participationId, String reasonCode, String reasonDetail) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    if (reportRepository.existsByParticipationIdAndReporterId(participationId, reporterId)) {
      throw new ConflictException("error.zone_event.social.already_reported");
    }

    ZoneEventReport report =
        reportRepository.save(
            ZoneEventReport.builder()
                .participation(participation)
                .reporterId(reporterId)
                .reasonCode(reasonCode)
                .reasonDetail(reasonDetail)
                .status(ReportStatus.OPEN)
                .build());

    long openReports =
        reportRepository.countByParticipationIdAndStatus(participationId, ReportStatus.OPEN);
    if (openReports >= 3) {
      participation.hide();
      participation.markUnderReview();
    }

    return ReportResDto.from(report);
  }

  /** Updates participation visibility (PUBLIC or PRIVATE). */
  @Transactional
  public void updateVisibility(
      UUID userId, UUID participationId, ParticipationVisibility visibility, UserRole userRole) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    if (!participation.getUserId().equals(userId) && !isOperator(userRole)) {
      throw new ForbiddenException("error.zone_event.participation.forbidden");
    }

    participation.changeVisibility(visibility);
  }

  private ZoneEventParticipation getPublicActiveParticipation(UUID participationId) {
    ZoneEventParticipation participation =
        participationRepository
            .findById(participationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("error.zone_event.participation.not_found"));

    if (participation.getStatus() != ParticipationStatus.SUCCESS
        || participation.getVisibility() != ParticipationVisibility.PUBLIC
        || Boolean.TRUE.equals(participation.getHidden())) {
      throw new ConflictException("error.zone_event.invalid_state");
    }
    return participation;
  }

  private CommentResDto buildCommentDto(ZoneEventComment comment, UUID currentUserId) {
    User author = userRepository.findById(comment.getUserId()).orElse(null);
    String nickname = author == null ? "Unknown" : author.getNickname();
    String profileUrl = author == null ? null : author.getProfileImageUrl();
    EquippedTitleResDto title = zoneTitleService.getEquippedTitle(comment.getUserId());
    boolean isMine = currentUserId != null && currentUserId.equals(comment.getUserId());

    return new CommentResDto(
        comment.getId(),
        comment.getParticipation().getId(),
        comment.getUserId(),
        nickname,
        profileUrl,
        title,
        comment.getContent(),
        comment.getCreatedAt(),
        isMine);
  }

  private boolean isOperator(UserRole role) {
    return role == UserRole.ADMIN || role == UserRole.MANAGER;
  }
}

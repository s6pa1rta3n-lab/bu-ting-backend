package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.butingbe.domain.user.entity.UserRole;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.response.CommentResDto;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ZoneEventSocialServiceTest {

  private static final UUID USER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID OTHER_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");
  private static final UUID PARTICIPATION_ID =
      UUID.fromString("33333333-0000-0000-0000-000000000003");
  private static final UUID COMMENT_ID = UUID.fromString("44444444-0000-0000-0000-000000000004");

  @Mock private ZoneEventParticipationRepository participationRepository;
  @Mock private ZoneEventLikeRepository likeRepository;
  @Mock private ZoneEventCommentRepository commentRepository;
  @Mock private ZoneEventReportRepository reportRepository;
  @Mock private UserRepository userRepository;
  @Mock private ZoneTitleService zoneTitleService;

  @InjectMocks private ZoneEventSocialService socialService;

  private ZoneEventParticipation makeParticipation(
      UUID authorId,
      ParticipationStatus status,
      ParticipationVisibility visibility,
      boolean hidden) {
    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .userId(authorId)
            .status(status)
            .visibility(visibility)
            .gpsLat(35.0)
            .gpsLng(129.0)
            .joinedAt(java.time.OffsetDateTime.now())
            .likeCount(0L)
            .build();
    ReflectionTestUtils.setField(p, "id", PARTICIPATION_ID);
    ReflectionTestUtils.setField(p, "hidden", hidden);
    return p;
  }

  @Test
  @DisplayName("Liking own participation throws IllegalArgumentException")
  void likeParticipation_ownParticipation() {
    ZoneEventParticipation p =
        makeParticipation(
            USER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> socialService.likeParticipation(USER_ID, PARTICIPATION_ID))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Liking non-public or non-success participation throws ConflictException")
  void likeParticipation_invalidState() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.UNDER_REVIEW, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> socialService.likeParticipation(USER_ID, PARTICIPATION_ID))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("Liking already liked participation does nothing")
  void likeParticipation_alreadyLiked() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));
    when(likeRepository.findByParticipationIdAndUserId(PARTICIPATION_ID, USER_ID))
        .thenReturn(Optional.of(ZoneEventLike.builder().participation(p).userId(USER_ID).build()));

    socialService.likeParticipation(USER_ID, PARTICIPATION_ID);

    assertThat(p.getLikeCount()).isEqualTo(0);
    verify(likeRepository, never()).save(any(ZoneEventLike.class));
  }

  @Test
  @DisplayName("Liking eligible participation saves like and increments count")
  void likeParticipation_success() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));
    when(likeRepository.findByParticipationIdAndUserId(PARTICIPATION_ID, USER_ID))
        .thenReturn(Optional.empty());

    socialService.likeParticipation(USER_ID, PARTICIPATION_ID);

    assertThat(p.getLikeCount()).isEqualTo(1);
    verify(likeRepository).save(any(ZoneEventLike.class));
  }

  @Test
  @DisplayName("Unliking existing like deletes like and decrements count")
  void unlikeParticipation_success() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    p.incrementLikeCount();
    ZoneEventLike like = ZoneEventLike.builder().participation(p).userId(USER_ID).build();

    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));
    when(likeRepository.findByParticipationIdAndUserId(PARTICIPATION_ID, USER_ID))
        .thenReturn(Optional.of(like));

    socialService.unlikeParticipation(USER_ID, PARTICIPATION_ID);

    assertThat(p.getLikeCount()).isEqualTo(0);
    verify(likeRepository).delete(like);
  }

  @Test
  @DisplayName("Adding comment increments count and returns dto")
  void addComment_success() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    ZoneEventComment comment =
        ZoneEventComment.builder().participation(p).userId(USER_ID).content("멋져요").build();
    ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
    when(commentRepository.save(any(ZoneEventComment.class))).thenReturn(comment);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    CommentResDto dto = socialService.addComment(USER_ID, PARTICIPATION_ID, "멋져요");

    assertThat(dto.content()).isEqualTo("멋져요");
    assertThat(p.getCommentCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Listing comments returns active comments")
  void getComments_success() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    ZoneEventComment comment =
        ZoneEventComment.builder().participation(p).userId(USER_ID).content("댓글").build();
    ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

    when(commentRepository.findByParticipationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            PARTICIPATION_ID))
        .thenReturn(List.of(comment));

    List<CommentResDto> comments = socialService.getComments(USER_ID, PARTICIPATION_ID);
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).isMine()).isTrue();
  }

  @Test
  @DisplayName("Updating comment by author succeeds")
  void updateComment_author() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    ZoneEventComment comment =
        ZoneEventComment.builder().participation(p).userId(USER_ID).content("원래 내용").build();
    when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

    CommentResDto res = socialService.updateComment(USER_ID, COMMENT_ID, "수정된 내용", UserRole.USER);

    assertThat(res.content()).isEqualTo("수정된 내용");
  }

  @Test
  @DisplayName("Updating comment by non-author and non-operator throws ForbiddenException")
  void updateComment_forbidden() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    ZoneEventComment comment =
        ZoneEventComment.builder().participation(p).userId(OTHER_ID).content("원래 내용").build();
    when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

    assertThatThrownBy(() -> socialService.updateComment(USER_ID, COMMENT_ID, "수정", UserRole.USER))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("Deleting comment by operator soft-deletes and decrements count")
  void deleteComment_operator() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    p.incrementCommentCount();
    ZoneEventComment comment =
        ZoneEventComment.builder().participation(p).userId(OTHER_ID).content("내용").build();
    when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

    socialService.deleteComment(USER_ID, COMMENT_ID, UserRole.ADMIN);

    assertThat(comment.isDeleted()).isTrue();
    assertThat(p.getCommentCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("Reporting already reported participation throws ConflictException")
  void reportParticipation_duplicate() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));
    when(reportRepository.existsByParticipationIdAndReporterId(PARTICIPATION_ID, USER_ID))
        .thenReturn(true);

    assertThatThrownBy(
            () -> socialService.reportParticipation(USER_ID, PARTICIPATION_ID, "SPAM", "스팸"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("Reporting reaching 3 open reports automatically hides and marks under review")
  void reportParticipation_autoHideAtThree() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));
    when(reportRepository.existsByParticipationIdAndReporterId(PARTICIPATION_ID, USER_ID))
        .thenReturn(false);

    ZoneEventReport report =
        ZoneEventReport.builder()
            .participation(p)
            .reporterId(USER_ID)
            .reasonCode("ABUSE")
            .reasonDetail("부적절")
            .status(ReportStatus.OPEN)
            .build();
    when(reportRepository.save(any(ZoneEventReport.class))).thenReturn(report);
    when(reportRepository.countByParticipationIdAndStatus(PARTICIPATION_ID, ReportStatus.OPEN))
        .thenReturn(3L);

    ReportResDto dto = socialService.reportParticipation(USER_ID, PARTICIPATION_ID, "ABUSE", "부적절");

    assertThat(dto.reasonCode()).isEqualTo("ABUSE");
    assertThat(p.getHidden()).isTrue();
    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.UNDER_REVIEW);
  }

  @Test
  @DisplayName("Updating visibility by owner changes visibility")
  void updateVisibility_owner() {
    ZoneEventParticipation p =
        makeParticipation(
            USER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    socialService.updateVisibility(
        USER_ID, PARTICIPATION_ID, ParticipationVisibility.PRIVATE, UserRole.USER);

    assertThat(p.getVisibility()).isEqualTo(ParticipationVisibility.PRIVATE);
  }

  @Test
  @DisplayName("Updating visibility by non-owner throws ForbiddenException")
  void updateVisibility_forbidden() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    assertThatThrownBy(
            () ->
                socialService.updateVisibility(
                    USER_ID, PARTICIPATION_ID, ParticipationVisibility.PRIVATE, UserRole.USER))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("Updating visibility by operator succeeds")
  void updateVisibility_operator() {
    ZoneEventParticipation p =
        makeParticipation(
            OTHER_ID, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, false);
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.of(p));

    socialService.updateVisibility(
        USER_ID, PARTICIPATION_ID, ParticipationVisibility.PRIVATE, UserRole.ADMIN);

    assertThat(p.getVisibility()).isEqualTo(ParticipationVisibility.PRIVATE);
  }

  @Test
  @DisplayName("ResourceNotFound exceptions in social operations")
  void socialOperations_notFound() {
    when(participationRepository.findById(PARTICIPATION_ID)).thenReturn(Optional.empty());
    when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> socialService.likeParticipation(USER_ID, PARTICIPATION_ID))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> socialService.unlikeParticipation(USER_ID, PARTICIPATION_ID))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () -> socialService.reportParticipation(USER_ID, PARTICIPATION_ID, "SPAM", null))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () ->
                socialService.updateVisibility(
                    USER_ID, PARTICIPATION_ID, ParticipationVisibility.PRIVATE, UserRole.USER))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> socialService.updateComment(USER_ID, COMMENT_ID, "내용", UserRole.USER))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> socialService.deleteComment(USER_ID, COMMENT_ID, UserRole.USER))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}

package com.butingbe.domain.zoneevent.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing a user comment on a zone event participation. */
@Entity
@Table(name = "zone_event_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZoneEventComment extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "comment_id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "participation_id", nullable = false)
  private ZoneEventParticipation participation;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @Builder
  private ZoneEventComment(ZoneEventParticipation participation, UUID userId, String content) {
    this.participation = participation;
    this.userId = userId;
    this.content = content;
  }

  /**
   * Updates comment content.
   *
   * @param content new comment text
   */
  public void updateContent(String content) {
    this.content = content;
  }

  /** Soft-deletes this comment by recording timestamp. */
  public void softDelete() {
    this.deletedAt = OffsetDateTime.now();
  }

  /**
   * Checks whether this comment is deleted.
   *
   * @return true if deleted
   */
  public boolean isDeleted() {
    return this.deletedAt != null;
  }
}

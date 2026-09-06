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
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing a user like on a zone event participation. */
@Entity
@Table(
    name = "zone_event_like",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_zone_event_like",
          columnNames = {"participation_id", "user_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZoneEventLike extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "like_id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "participation_id", nullable = false)
  private ZoneEventParticipation participation;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Builder
  private ZoneEventLike(ZoneEventParticipation participation, UUID userId) {
    this.participation = participation;
    this.userId = userId;
  }
}

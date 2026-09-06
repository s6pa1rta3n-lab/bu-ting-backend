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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity representing a zone title earned by a user. */
@Entity
@Table(
    name = "user_zone_title",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_user_zone_title",
          columnNames = {"user_id", "title_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserZoneTitle extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "user_title_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "title_id", nullable = false)
  private ZoneTitleDef titleDef;

  @Column(name = "is_equipped", nullable = false)
  private Boolean isEquipped;

  @Column(name = "earned_at", nullable = false)
  private OffsetDateTime earnedAt;

  @Builder
  private UserZoneTitle(
      UUID userId, ZoneTitleDef titleDef, Boolean isEquipped, OffsetDateTime earnedAt) {
    this.userId = userId;
    this.titleDef = titleDef;
    this.isEquipped = isEquipped != null && isEquipped;
    this.earnedAt = earnedAt == null ? OffsetDateTime.now() : earnedAt;
  }

  /** Equips this title. */
  public void equip() {
    this.isEquipped = true;
  }

  /** Unequips this title. */
  public void unequip() {
    this.isEquipped = false;
  }
}

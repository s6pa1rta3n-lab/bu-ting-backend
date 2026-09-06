package com.butingbe.domain.zoneevent.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** History record of a user achieving an explorer city grade. */
@Entity
@Table(name = "user_city_grade_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCityGradeHistory extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "history_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CityGrade grade;

  @Column(name = "reached_at", nullable = false)
  private OffsetDateTime reachedAt;

  @Builder
  private UserCityGradeHistory(UUID userId, CityGrade grade, OffsetDateTime reachedAt) {
    this.userId = userId;
    this.grade = grade;
    this.reachedAt = reachedAt == null ? OffsetDateTime.now() : reachedAt;
  }
}

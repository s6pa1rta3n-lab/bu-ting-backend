package com.butingbe.domain.zoneevent.entity;

import com.butingbe.global.common.TimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Definition of an earnable zone title per zone and tier. */
@Entity
@Table(name = "zone_title_def")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZoneTitleDef extends TimestampEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "title_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "zone_id", nullable = false, length = 30)
  private String zoneId;

  @Column(nullable = false)
  private Integer tier;

  @Column(name = "required_success_count", nullable = false)
  private Integer requiredSuccessCount;

  @Column(name = "title_code", nullable = false, unique = true, length = 100)
  private String titleCode;

  @Column(name = "title_name", nullable = false, length = 100)
  private String titleName;

  @Column(length = 50)
  private String style;

  @Column(length = 50)
  private String color;

  @Builder
  private ZoneTitleDef(
      String zoneId,
      Integer tier,
      Integer requiredSuccessCount,
      String titleCode,
      String titleName,
      String style,
      String color) {
    this.zoneId = zoneId;
    this.tier = tier;
    this.requiredSuccessCount = requiredSuccessCount;
    this.titleCode = titleCode;
    this.titleName = titleName;
    this.style = style;
    this.color = color;
  }
}

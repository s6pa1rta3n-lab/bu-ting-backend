package com.butingbe.domain.zoneevent.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZoneEventEntityTest {

  @Test
  @DisplayName("ZoneEvent 엔티티 생성 및 상태 변경 메서드 검증")
  void zoneEventMethods() {
    ZoneEventType type =
        ZoneEventType.builder()
            .typeCode("PLACE_AUTH")
            .name("장소 인증")
            .description("설명")
            .requiresUpload(true)
            .build();

    RewardSnapshot baseReward = new RewardSnapshot(50, "POINT_BASE", null, null);
    RewardSnapshot excelReward = new RewardSnapshot(100, "EXCELLENCE_BADGE", null, null);

    ZoneEvent event =
        ZoneEvent.builder()
            .zoneId("SUYEONG_NAMGU")
            .type(type)
            .title("광안리 축제")
            .description("상세 설명")
            .startsAt(OffsetDateTime.now())
            .durationMinutes(60)
            .status(ZoneEventStatus.SCHEDULED)
            .baseReward(baseReward)
            .excellenceReward(excelReward)
            .successLimitPerUser(1)
            .build();

    assertThat(event.getZoneId()).isEqualTo("SUYEONG_NAMGU");
    assertThat(event.getTitle()).isEqualTo("광안리 축제");
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.SCHEDULED);

    event.activate();
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.ACTIVE);

    event.close();
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.CLOSED);

    event.cancel();
    assertThat(event.getStatus()).isEqualTo(ZoneEventStatus.CANCELLED);

    RewardSnapshot updatedExcel = new RewardSnapshot(150, "NEW_BADGE", null, null);
    event.updateActive("수정된 제목", "수정된 설명", Integer.valueOf(90), updatedExcel, Integer.valueOf(2));
    assertThat(event.getTitle()).isEqualTo("수정된 제목");
    assertThat(event.getDescription()).isEqualTo("수정된 설명");
    assertThat(event.getDurationMinutes()).isEqualTo(90);
    assertThat(event.getExcellenceReward()).isEqualTo(updatedExcel);
    assertThat(event.getSuccessLimitPerUser()).isEqualTo(2);

    ZoneEventType type2 =
        ZoneEventType.builder()
            .typeCode("STAMP")
            .name("스탬프")
            .description("스탬프")
            .requiresUpload(false)
            .build();

    OffsetDateTime newStartsAt = OffsetDateTime.now().plusDays(1);
    RewardSnapshot newBase = new RewardSnapshot(100, "NEW_BASE", null, null);
    event.updateScheduled(
        "HAEUNDAE_GIJANG", type2, "해운대 행사", "새 설명", newStartsAt, 120, newBase, updatedExcel, 3);

    assertThat(event.getZoneId()).isEqualTo("HAEUNDAE_GIJANG");
    assertThat(event.getType().getTypeCode()).isEqualTo("STAMP");
    assertThat(event.getTitle()).isEqualTo("해운대 행사");
    assertThat(event.getDescription()).isEqualTo("새 설명");
    assertThat(event.getStartsAt()).isEqualTo(newStartsAt);
    assertThat(event.getDurationMinutes()).isEqualTo(120);
    assertThat(event.getBaseReward()).isEqualTo(newBase);
    assertThat(event.getSuccessLimitPerUser()).isEqualTo(3);
  }

  @Test
  @DisplayName("ZoneEventAuthTarget 엔티티 수정 메서드 검증")
  void zoneEventAuthTargetMethods() {
    ZoneEventAuthTarget target =
        ZoneEventAuthTarget.builder()
            .targetKind(ZoneEventTargetKind.PLACE)
            .landmarkId("p1")
            .placeName("광안리")
            .guideText("가이드")
            .exampleFileKey("example.jpg")
            .latitude(35.1)
            .longitude(129.1)
            .radiusM(50)
            .build();

    assertThat(target.getPlaceName()).isEqualTo("광안리");
    assertThat(target.getRadiusM()).isEqualTo(50);

    target.update(
        ZoneEventTargetKind.OBJECT, "p2", "해운대", "새 가이드", "new_example.jpg", 35.2, 129.2, 100);

    assertThat(target.getTargetKind()).isEqualTo(ZoneEventTargetKind.OBJECT);
    assertThat(target.getLandmarkId()).isEqualTo("p2");
    assertThat(target.getPlaceName()).isEqualTo("해운대");
    assertThat(target.getGuideText()).isEqualTo("새 가이드");
    assertThat(target.getExampleFileKey()).isEqualTo("new_example.jpg");
    assertThat(target.getLatitude()).isEqualTo(35.2);
    assertThat(target.getLongitude()).isEqualTo(129.2);
    assertThat(target.getRadiusM()).isEqualTo(100);
  }

  @Test
  @DisplayName("ZoneEventParticipation 엔티티 취소 메서드 검증")
  void zoneEventParticipationMethods() {
    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .userId(java.util.UUID.randomUUID())
            .status(ParticipationStatus.JOINED)
            .joinedAt(OffsetDateTime.now())
            .build();

    p.cancel("USER_CANCELLED");
    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.CANCELLED);
    assertThat(p.getCancelReason()).isEqualTo("USER_CANCELLED");
  }
}

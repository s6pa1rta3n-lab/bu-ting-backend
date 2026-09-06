package com.butingbe.domain.zoneevent.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.butingbe.domain.notification.entity.DeviceType;
import com.butingbe.domain.notification.entity.PushNotificationLog;
import com.butingbe.domain.notification.entity.PushNotificationStatus;
import com.butingbe.domain.notification.entity.UserDeviceToken;
import com.butingbe.domain.notification.entity.UserNotificationSetting;
import com.butingbe.domain.notification.entity.UserZoneSubscription;
import com.butingbe.domain.notification.entity.UserZoneSubscriptionId;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZoneEventEntityUnitTest {

  @Test
  @DisplayName("ZoneEventParticipation state transitions and counter mutations")
  void testZoneEventParticipation() {
    ZoneEvent event = ZoneEvent.builder().title("이벤트").zoneId("GWANGAN").build();
    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(UUID.randomUUID())
            .status(ParticipationStatus.SUBMITTED)
            .visibility(ParticipationVisibility.PUBLIC)
            .likeCount(0L)
            .gpsLat(35.0)
            .gpsLng(129.0)
            .joinedAt(OffsetDateTime.now())
            .build();

    p.markUnderReview();
    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.UNDER_REVIEW);

    p.markSuccess();
    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.SUCCESS);
    assertThat(p.getCompletedAt()).isNotNull();

    p.hide();
    assertThat(p.getHidden()).isTrue();

    p.unhide();
    assertThat(p.getHidden()).isFalse();

    UUID reviewerId = UUID.randomUUID();
    p.revoke(reviewerId);
    assertThat(p.getStatus()).isEqualTo(ParticipationStatus.REVOKED);
    assertThat(p.getReviewedBy()).isEqualTo(reviewerId);

    p.incrementLikeCount();
    assertThat(p.getLikeCount()).isEqualTo(1L);
    p.decrementLikeCount();
    assertThat(p.getLikeCount()).isEqualTo(0L);
    p.decrementLikeCount();
    assertThat(p.getLikeCount()).isEqualTo(0L);

    p.incrementCommentCount();
    assertThat(p.getCommentCount()).isEqualTo(1);
    p.decrementCommentCount();
    assertThat(p.getCommentCount()).isEqualTo(0);
    p.decrementCommentCount();
    assertThat(p.getCommentCount()).isEqualTo(0);

    p.changeVisibility(ParticipationVisibility.PRIVATE);
    assertThat(p.getVisibility()).isEqualTo(ParticipationVisibility.PRIVATE);
  }

  @Test
  @DisplayName("ZoneEventReport resolve and dismiss transitions")
  void testZoneEventReport() {
    ZoneEventReport report =
        ZoneEventReport.builder()
            .reporterId(UUID.randomUUID())
            .reasonCode("SPAM")
            .status(ReportStatus.OPEN)
            .build();

    report.resolve();
    assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);

    report.dismiss();
    assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
  }

  @Test
  @DisplayName("ZoneEventComment soft delete")
  void testZoneEventComment() {
    ZoneEventComment comment =
        ZoneEventComment.builder().userId(UUID.randomUUID()).content("댓글").build();

    comment.softDelete();
    assertThat(comment.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("UserZoneTitle equip and unequip")
  void testUserZoneTitle() {
    ZoneTitleDef def =
        ZoneTitleDef.builder()
            .zoneId("GWANGAN")
            .tier(1)
            .requiredSuccessCount(3)
            .titleCode("TITLE_GWANGAN_EXPLORER")
            .titleName("광안리 탐험가")
            .build();

    UserZoneTitle title =
        UserZoneTitle.builder().userId(UUID.randomUUID()).titleDef(def).isEquipped(false).build();

    title.equip();
    assertThat(title.getIsEquipped()).isTrue();

    title.unequip();
    assertThat(title.getIsEquipped()).isFalse();
  }

  @Test
  @DisplayName("UserNotificationSetting updateSettings")
  void testUserNotificationSetting() {
    UserNotificationSetting setting =
        UserNotificationSetting.builder().userId(UUID.randomUUID()).build();

    setting.updateSettings(false, true, false);
    assertThat(setting.getPushEnabled()).isFalse();
    assertThat(setting.getZoneEventEnabled()).isTrue();
    assertThat(setting.getSettlementEnabled()).isFalse();
  }

  @Test
  @DisplayName("UserZoneSubscription activate and deactivate")
  void testUserZoneSubscription() {
    UUID userId = UUID.randomUUID();
    UserZoneSubscription sub =
        UserZoneSubscription.builder().userId(userId).zoneId("GWANGAN").isActive(false).build();

    sub.activate();
    assertThat(sub.getIsActive()).isTrue();

    sub.deactivate();
    assertThat(sub.getIsActive()).isFalse();

    UserZoneSubscriptionId id1 = new UserZoneSubscriptionId(userId, "GWANGAN");
    UserZoneSubscriptionId id2 = new UserZoneSubscriptionId(userId, "GWANGAN");
    assertThat(id1).isEqualTo(id2);
    assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    assertThat(id1.getUserId()).isEqualTo(userId);
    assertThat(id1.getZoneId()).isEqualTo("GWANGAN");
  }

  @Test
  @DisplayName("UserDeviceToken updateLastSeen")
  void testUserDeviceToken() {
    UserDeviceToken token =
        UserDeviceToken.builder()
            .userId(UUID.randomUUID())
            .fcmToken("token123")
            .deviceType(DeviceType.ANDROID)
            .lastSeenAt(OffsetDateTime.now().minusHours(1))
            .build();

    token.updateLastSeen();
    assertThat(token.getLastSeenAt()).isNotNull();
  }

  @Test
  @DisplayName("ZoneEventRound and Slot transitions")
  void testZoneEventRound() {
    OffsetDateTime start = OffsetDateTime.now();
    OffsetDateTime end = start.plusDays(7);
    ZoneEventRound round =
        ZoneEventRound.builder()
            .roundType(RoundType.REGULAR)
            .startsAt(start)
            .endsAt(end)
            .status(RoundStatus.SCHEDULED)
            .build();

    round.open();
    assertThat(round.getStatus()).isEqualTo(RoundStatus.OPEN);

    round.close();
    assertThat(round.getStatus()).isEqualTo(RoundStatus.CLOSED);

    OffsetDateTime settledAt = OffsetDateTime.now();
    round.settle(settledAt);
    assertThat(round.getStatus()).isEqualTo(RoundStatus.SETTLED);
    assertThat(round.getSettledAt()).isEqualTo(settledAt);

    ZoneEventRoundSlot slot =
        ZoneEventRoundSlot.builder().round(round).zoneId("GWANGAN").slotKind(SlotKind.AUTH).build();
    UUID newEventId = UUID.randomUUID();
    slot.assignEvent(newEventId);
    assertThat(slot.getEventId()).isEqualTo(newEventId);
  }

  @Test
  @DisplayName("ZoneEventAuthTarget update")
  void testZoneEventAuthTarget() {
    ZoneEventAuthTarget target =
        ZoneEventAuthTarget.builder()
            .placeName("원래")
            .guideText("안내")
            .latitude(35.0)
            .longitude(129.0)
            .radiusM(50)
            .build();

    target.update("새장소", "새안내", "새파일", 35.1, 129.1, 80);
    assertThat(target.getPlaceName()).isEqualTo("새장소");
    assertThat(target.getRadiusM()).isEqualTo(80);
  }

  @Test
  @DisplayName("Entity DTOs and value objects instantiation")
  void testOtherEntities() {
    UserCityGradeHistory history =
        UserCityGradeHistory.builder()
            .userId(UUID.randomUUID())
            .grade(CityGrade.MASTER)
            .reachedAt(OffsetDateTime.now())
            .build();
    assertThat(history.getGrade()).isEqualTo(CityGrade.MASTER);

    PushNotificationLog log =
        PushNotificationLog.builder()
            .userId(UUID.randomUUID())
            .title("제목")
            .body("본문")
            .status(PushNotificationStatus.SENT)
            .sentAt(OffsetDateTime.now())
            .build();
    assertThat(log.getStatus()).isEqualTo(PushNotificationStatus.SENT);

    ZoneEventAuditLog audit =
        ZoneEventAuditLog.builder()
            .operatorId(UUID.randomUUID())
            .action("ACTION")
            .targetType("TARGET")
            .targetId("123")
            .details("상세")
            .build();
    assertThat(audit.getAction()).isEqualTo("ACTION");

    ZoneEventSettlementReport report =
        ZoneEventSettlementReport.builder()
            .roundId(UUID.randomUUID())
            .summaryJson(Map.of("k", "v"))
            .build();
    assertThat(report.getRoundId()).isNotNull();

    ZoneTitleDef def =
        ZoneTitleDef.builder()
            .titleCode("CODE")
            .titleName("이름")
            .zoneId("GWANGAN")
            .tier(1)
            .requiredSuccessCount(3)
            .build();
    assertThat(def.getTitleCode()).isEqualTo("CODE");
  }
}

package com.butingbe.domain.zoneevent.dto.response;

import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 참여 로그 응답.
 *
 * <p>참여 시작 시점에는 {@code distanceM}(타겟까지 거리)을 채우고 미디어·완료 시각·보상은 비어 있다. 후속 이슈(제출·판정)에서 채워진다.
 */
public record ParticipationResDto(
    String participationId,
    String eventId,
    String zoneId,
    String typeCode,
    String status,
    Boolean success,
    Integer distanceM,
    String mediaUrl,
    String content,
    long likeCount,
    String visibility,
    OffsetDateTime joinedAt,
    OffsetDateTime completedAt,
    List<Object> rewards) {

  public static ParticipationResDto of(ZoneEventParticipation participation, Integer distanceM) {
    return of(participation, distanceM, null, List.of());
  }

  /**
   * 참여 엔티티를 응답 DTO로 변환한다.
   *
   * @param participation 참여 엔티티
   * @param distanceM 인증 타겟과의 거리(미터)
   * @param mediaUrl 미디어 사전 서명 URL
   * @param rewards 지급된 보상 목록
   * @return 참여 응답 DTO
   */
  @SuppressWarnings("unchecked")
  public static ParticipationResDto of(
      ZoneEventParticipation participation, Integer distanceM, String mediaUrl, List<?> rewards) {
    return new ParticipationResDto(
        participation.getId().toString(),
        participation.getEvent().getId().toString(),
        participation.getEvent().getZoneId(),
        participation.getEvent().getType().getTypeCode(),
        participation.getStatus().name(),
        participation.getSuccess(),
        distanceM,
        mediaUrl,
        participation.getContent(),
        participation.getLikeCount(),
        participation.getVisibility().name(),
        participation.getJoinedAt(),
        participation.getCompletedAt(),
        rewards == null ? List.of() : (List<Object>) rewards);
  }
}

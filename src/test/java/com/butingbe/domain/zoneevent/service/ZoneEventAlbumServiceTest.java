package com.butingbe.domain.zoneevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.request.AlbumSort;
import com.butingbe.domain.zoneevent.dto.response.AlbumPageResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.entity.ZoneEvent;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.repository.ZoneEventLikeRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ZoneEventAlbumServiceTest {

  private static final UUID USER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID EVENT_ID = UUID.fromString("22222222-0000-0000-0000-000000000002");
  private static final UUID ROUND_ID = UUID.fromString("33333333-0000-0000-0000-000000000003");
  private static final String ZONE_ID = "GWANGAN";

  @Mock private ZoneEventParticipationRepository participationRepository;
  @Mock private ZoneEventLikeRepository likeRepository;
  @Mock private UserRepository userRepository;
  @Mock private ZoneTitleService zoneTitleService;
  @Mock private FileStorageService fileStorageService;

  @InjectMocks private ZoneEventAlbumService albumService;

  private ZoneEventParticipation makeParticipation(UUID id, String mediaKey) {
    ZoneEvent event = ZoneEvent.builder().title("이벤트").zoneId(ZONE_ID).build();
    ReflectionTestUtils.setField(event, "id", EVENT_ID);

    ZoneEventParticipation p =
        ZoneEventParticipation.builder()
            .event(event)
            .userId(USER_ID)
            .status(ParticipationStatus.SUCCESS)
            .visibility(ParticipationVisibility.PUBLIC)
            .gpsLat(35.0)
            .gpsLng(129.0)
            .joinedAt(java.time.OffsetDateTime.now())
            .likeCount(5L)
            .build();
    ReflectionTestUtils.setField(p, "id", id);
    ReflectionTestUtils.setField(p, "hidden", false);
    ReflectionTestUtils.setField(p, "mediaFileKey", mediaKey);
    ReflectionTestUtils.setField(p, "content", "내용");
    ReflectionTestUtils.setField(p, "commentCount", 2);
    ReflectionTestUtils.setField(p, "completedAt", java.time.OffsetDateTime.now());
    return p;
  }

  @Test
  @DisplayName("getEventAlbum returns paginated items with MOST_LIKED sort")
  void getEventAlbum_mostLiked() {
    UUID pid = UUID.randomUUID();
    ZoneEventParticipation p = makeParticipation(pid, "media/file.jpg");
    Pageable pageable = PageRequest.of(0, 10);
    PageImpl<ZoneEventParticipation> page = new PageImpl<>(List.of(p), pageable, 20);

    when(participationRepository.findByEvent_IdAndStatusAndVisibilityAndHiddenFalse(
            eq(EVENT_ID),
            eq(ParticipationStatus.SUCCESS),
            eq(ParticipationVisibility.PUBLIC),
            any()))
        .thenReturn(page);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
    when(likeRepository.existsByParticipationIdAndUserId(pid, USER_ID)).thenReturn(true);
    when(fileStorageService.getPresignedUrl("media/file.jpg")).thenReturn("http://presigned.url");

    AlbumPageResDto result =
        albumService.getEventAlbum(EVENT_ID, null, 10, AlbumSort.MOST_LIKED, USER_ID);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).mediaUrl()).isEqualTo("http://presigned.url");
    assertThat(result.items().get(0).likedByMe()).isTrue();
    assertThat(result.items().get(0).isMine()).isTrue();
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("getZoneAlbum returns items with LATEST sort and handles presigned URL exception")
  void getZoneAlbum_latest() {
    UUID pid = UUID.randomUUID();
    ZoneEventParticipation p = makeParticipation(pid, "invalid/key.jpg");
    Pageable pageable = PageRequest.of(0, 10);
    PageImpl<ZoneEventParticipation> page = new PageImpl<>(List.of(p), pageable, 1);

    when(participationRepository.findByEvent_ZoneIdAndStatusAndVisibilityAndHiddenFalse(
            eq(ZONE_ID),
            eq(ParticipationStatus.SUCCESS),
            eq(ParticipationVisibility.PUBLIC),
            any()))
        .thenReturn(page);
    when(fileStorageService.getPresignedUrl("invalid/key.jpg"))
        .thenThrow(new RuntimeException("S3 error"));

    String cursor = Base64.getUrlEncoder().encodeToString("0".getBytes(StandardCharsets.UTF_8));
    AlbumPageResDto result = albumService.getZoneAlbum(ZONE_ID, cursor, 10, AlbumSort.LATEST, null);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).mediaUrl()).isNull();
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
  }

  @Test
  @DisplayName("getRoundAlbum returns items with corrupted cursor fallback")
  void getRoundAlbum_corruptedCursor() {
    UUID pid = UUID.randomUUID();
    ZoneEventParticipation p = makeParticipation(pid, null);
    Pageable pageable = PageRequest.of(0, 10);
    PageImpl<ZoneEventParticipation> page = new PageImpl<>(List.of(p), pageable, 1);

    when(participationRepository.findByEvent_RoundIdAndStatusAndVisibilityAndHiddenFalse(
            eq(ROUND_ID),
            eq(ParticipationStatus.SUCCESS),
            eq(ParticipationVisibility.PUBLIC),
            any()))
        .thenReturn(page);

    AlbumPageResDto result =
        albumService.getRoundAlbum(ROUND_ID, "not-valid-base64-!!!", 10, AlbumSort.LATEST, null);

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).mediaUrl()).isNull();
  }
}

package com.butingbe.domain.zoneevent.service;

import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.user.entity.User;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.request.AlbumSort;
import com.butingbe.domain.zoneevent.dto.response.AlbumItemResDto;
import com.butingbe.domain.zoneevent.dto.response.AlbumPageResDto;
import com.butingbe.domain.zoneevent.dto.response.EquippedTitleResDto;
import com.butingbe.domain.zoneevent.entity.ParticipationStatus;
import com.butingbe.domain.zoneevent.entity.ParticipationVisibility;
import com.butingbe.domain.zoneevent.entity.ZoneEventParticipation;
import com.butingbe.domain.zoneevent.repository.ZoneEventLikeRepository;
import com.butingbe.domain.zoneevent.repository.ZoneEventParticipationRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service providing public album feed queries scoped to events, zones, or rounds. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneEventAlbumService {

  private final ZoneEventParticipationRepository participationRepository;
  private final ZoneEventLikeRepository likeRepository;
  private final UserRepository userRepository;
  private final ZoneTitleService zoneTitleService;
  private final FileStorageService fileStorageService;

  /** Retrieves album feed for a specific zone event. */
  @Transactional(readOnly = true)
  public AlbumPageResDto getEventAlbum(
      UUID eventId, String cursor, int size, AlbumSort sort, UUID currentUserId) {
    Pageable pageable = createPageable(cursor, size, sort);
    Page<ZoneEventParticipation> page =
        participationRepository.findByEvent_IdAndStatusAndVisibilityAndHiddenFalse(
            eventId, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, pageable);
    return toAlbumPage(page, currentUserId);
  }

  /** Retrieves album feed across all events in a specific zone. */
  @Transactional(readOnly = true)
  public AlbumPageResDto getZoneAlbum(
      String zoneId, String cursor, int size, AlbumSort sort, UUID currentUserId) {
    Pageable pageable = createPageable(cursor, size, sort);
    Page<ZoneEventParticipation> page =
        participationRepository.findByEvent_ZoneIdAndStatusAndVisibilityAndHiddenFalse(
            zoneId, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, pageable);
    return toAlbumPage(page, currentUserId);
  }

  /** Retrieves album feed across all events in a specific operating round. */
  @Transactional(readOnly = true)
  public AlbumPageResDto getRoundAlbum(
      UUID roundId, String cursor, int size, AlbumSort sort, UUID currentUserId) {
    Pageable pageable = createPageable(cursor, size, sort);
    Page<ZoneEventParticipation> page =
        participationRepository.findByEvent_RoundIdAndStatusAndVisibilityAndHiddenFalse(
            roundId, ParticipationStatus.SUCCESS, ParticipationVisibility.PUBLIC, pageable);
    return toAlbumPage(page, currentUserId);
  }

  private Pageable createPageable(String cursor, int size, AlbumSort sort) {
    int pageSize = Math.min(Math.max(size, 1), 50);
    int pageNumber = decodeCursor(cursor);

    Sort pageSort =
        (sort == AlbumSort.MOST_LIKED)
            ? Sort.by(Sort.Direction.DESC, "likeCount")
                .and(Sort.by(Sort.Direction.ASC, "completedAt"))
            : Sort.by(Sort.Direction.DESC, "completedAt").and(Sort.by(Sort.Direction.DESC, "id"));

    return PageRequest.of(pageNumber, pageSize, pageSort);
  }

  private AlbumPageResDto toAlbumPage(Page<ZoneEventParticipation> page, UUID currentUserId) {
    List<AlbumItemResDto> items =
        page.getContent().stream()
            .map(p -> toAlbumItem(p, currentUserId))
            .collect(Collectors.toList());

    String nextCursor = page.hasNext() ? encodeCursor(page.getNumber() + 1) : null;
    return new AlbumPageResDto(items, nextCursor, page.hasNext());
  }

  private AlbumItemResDto toAlbumItem(ZoneEventParticipation p, UUID currentUserId) {
    User author = userRepository.findById(p.getUserId()).orElse(null);
    String authorNickname = author == null ? "Unknown" : author.getNickname();
    String profileImageUrl = author == null ? null : author.getProfileImageUrl();
    EquippedTitleResDto equippedTitle = zoneTitleService.getEquippedTitle(p.getUserId());

    String mediaUrl = resolveMediaUrl(p.getMediaFileKey());
    boolean likedByMe =
        currentUserId != null
            && likeRepository.existsByParticipationIdAndUserId(p.getId(), currentUserId);
    boolean isMine = currentUserId != null && currentUserId.equals(p.getUserId());

    return new AlbumItemResDto(
        p.getId(),
        p.getEvent().getId(),
        p.getEvent().getTitle(),
        p.getEvent().getZoneId(),
        p.getUserId(),
        authorNickname,
        profileImageUrl,
        equippedTitle,
        mediaUrl,
        p.getContent(),
        p.getLikeCount(),
        p.getCommentCount(),
        p.getCompletedAt(),
        likedByMe,
        isMine);
  }

  private String resolveMediaUrl(String mediaFileKey) {
    if (mediaFileKey == null || mediaFileKey.isBlank()) {
      return null;
    }
    try {
      return fileStorageService.getPresignedUrl(mediaFileKey);
    } catch (Exception e) {
      log.debug("Failed to obtain presigned URL for media file: {}", mediaFileKey);
      return null;
    }
  }

  private int decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return 0;
    }
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      return Integer.parseInt(decoded);
    } catch (Exception e) {
      return 0;
    }
  }

  private String encodeCursor(int page) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(String.valueOf(page).getBytes(StandardCharsets.UTF_8));
  }
}

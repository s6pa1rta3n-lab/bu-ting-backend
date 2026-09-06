package com.butingbe.domain.reward.service;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.dto.request.RewardCatalogCreateReqDto;
import com.butingbe.domain.reward.dto.request.RewardCatalogUpdateReqDto;
import com.butingbe.domain.reward.dto.response.GrantedRewardDto;
import com.butingbe.domain.reward.dto.response.RewardCatalogResDto;
import com.butingbe.domain.reward.dto.response.RewardGrantPageResDto;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.repository.RewardCatalogRepository;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.global.error.exception.DuplicateResourceException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 보상 카탈로그 및 발급 이력 관리 서비스. */
@Service
@RequiredArgsConstructor
public class AdminRewardCatalogService {

  private final RewardCatalogRepository rewardCatalogRepository;
  private final RewardGrantRepository rewardGrantRepository;
  private final FileStorageService fileStorageService;

  /**
   * 보상 카탈로그 목록을 필터링하여 조회한다.
   *
   * @param user 인증된 관리자 유저
   * @param type 보상 구분 필터
   * @param active 활성화 상태 필터
   * @return 카탈로그 목록
   */
  @Transactional(readOnly = true)
  public List<RewardCatalogResDto> getCatalog(
      AuthenticatedUser user, RewardType type, Boolean active) {
    verifyOperator(user);

    Specification<RewardCatalog> spec =
        (root, query, builder) -> {
          var predicate = builder.conjunction();
          if (type != null) {
            predicate = builder.and(predicate, builder.equal(root.get("rewardType"), type));
          }
          if (active != null) {
            predicate = builder.and(predicate, builder.equal(root.get("active"), active));
          }
          return predicate;
        };

    List<RewardCatalog> catalogs =
        rewardCatalogRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "code"));

    return catalogs.stream()
        .map(
            c -> {
              String imageUrl =
                  c.getImageFileKey() != null
                      ? fileStorageService.getPresignedUrl(c.getImageFileKey())
                      : null;
              return RewardCatalogResDto.of(c, imageUrl);
            })
        .toList();
  }

  /**
   * 신규 보상 카탈로그 항목을 생성한다.
   *
   * @param user 인증된 관리자 유저
   * @param req 생성 요청 데이터
   * @return 생성된 보상 카탈로그 상세
   */
  @Transactional
  public RewardCatalogResDto createCatalog(AuthenticatedUser user, RewardCatalogCreateReqDto req) {
    verifyOperator(user);

    if (rewardCatalogRepository.existsByCode(req.code())) {
      throw new DuplicateResourceException("error.reward.code_duplicate");
    }

    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(req.rewardType())
            .code(req.code())
            .name(req.name())
            .pointAmount(req.pointAmount())
            .imageFileKey(req.imageFileKey())
            .stock(req.stock())
            .monthlyCap(req.monthlyCap())
            .validDays(req.validDays())
            .active(req.active() == null || req.active())
            .build();

    RewardCatalog saved = rewardCatalogRepository.save(catalog);
    String imageUrl =
        saved.getImageFileKey() != null
            ? fileStorageService.getPresignedUrl(saved.getImageFileKey())
            : null;

    return RewardCatalogResDto.of(saved, imageUrl);
  }

  /**
   * 보상 카탈로그의 이름, 재고, 월별 한도, 활성 상태를 수정한다.
   *
   * @param user 인증된 관리자 유저
   * @param rewardId 보상 식별자
   * @param req 수정 요청 데이터
   * @return 수정된 보상 카탈로그 상세
   */
  @Transactional
  public RewardCatalogResDto updateCatalog(
      AuthenticatedUser user, UUID rewardId, RewardCatalogUpdateReqDto req) {
    verifyOperator(user);

    RewardCatalog catalog =
        rewardCatalogRepository
            .findById(rewardId)
            .orElseThrow(() -> new ResourceNotFoundException("error.reward.not_found"));

    catalog.update(req.name(), req.stock(), req.monthlyCap(), req.active());

    String imageUrl =
        catalog.getImageFileKey() != null
            ? fileStorageService.getPresignedUrl(catalog.getImageFileKey())
            : null;

    return RewardCatalogResDto.of(catalog, imageUrl);
  }

  /**
   * 특정 보상 항목의 발급 이력을 커서 기반 페이징으로 조회한다.
   *
   * @param user 인증된 관리자 유저
   * @param rewardId 보상 식별자
   * @param cursor 커서 문자열
   * @param size 페이지 크기
   * @return 보상 발급 이력 커서 페이징 응답
   */
  @Transactional(readOnly = true)
  public RewardGrantPageResDto getGrants(
      AuthenticatedUser user, UUID rewardId, String cursor, int size) {
    verifyOperator(user);

    if (!rewardCatalogRepository.existsById(rewardId)) {
      throw new ResourceNotFoundException("error.reward.not_found");
    }

    int pageSize = size <= 0 ? 20 : Math.min(size, 100);
    CursorDecoded decodedCursor = decodeCursor(cursor);

    Specification<RewardGrant> spec =
        (root, query, builder) -> {
          var predicate = builder.equal(root.get("reward").get("id"), rewardId);
          if (decodedCursor != null) {
            var timeLess = builder.lessThan(root.get("grantedAt"), decodedCursor.grantedAt());
            var timeEqual = builder.equal(root.get("grantedAt"), decodedCursor.grantedAt());
            var idLess = builder.lessThan(root.get("id"), decodedCursor.id());
            predicate =
                builder.and(predicate, builder.or(timeLess, builder.and(timeEqual, idLess)));
          }
          return predicate;
        };

    PageRequest pageRequest =
        PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "grantedAt", "id"));
    List<RewardGrant> fetched = rewardGrantRepository.findAll(spec, pageRequest).getContent();

    boolean hasNext = fetched.size() > pageSize;
    List<RewardGrant> items = hasNext ? fetched.subList(0, pageSize) : fetched;

    String nextCursor = null;
    if (hasNext && !items.isEmpty()) {
      RewardGrant last = items.get(items.size() - 1);
      nextCursor = encodeCursor(last.getGrantedAt(), last.getId());
    }

    List<GrantedRewardDto> dtoList = items.stream().map(GrantedRewardDto::of).toList();

    return new RewardGrantPageResDto(dtoList, nextCursor, hasNext);
  }

  private String encodeCursor(OffsetDateTime grantedAt, UUID id) {
    String raw = grantedAt.toString() + "_" + id.toString();
    return Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  private CursorDecoded decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      int idx = raw.lastIndexOf('_');
      if (idx <= 0) {
        throw new IllegalArgumentException("error.common.invalid_cursor");
      }
      OffsetDateTime grantedAt = OffsetDateTime.parse(raw.substring(0, idx));
      UUID id = UUID.fromString(raw.substring(idx + 1));
      return new CursorDecoded(grantedAt, id);
    } catch (Exception e) {
      throw new IllegalArgumentException("error.common.invalid_cursor", e);
    }
  }

  private record CursorDecoded(OffsetDateTime grantedAt, UUID id) {}

  private void verifyOperator(AuthenticatedUser user) {
    if (user == null || (user.id() == null && !user.isDevelopmentAdmin())) {
      throw new UnauthenticatedException();
    }
    boolean isOp =
        user.isDevelopmentAdmin()
            || (user.authorities() != null
                && user.authorities().stream()
                    .anyMatch(
                        a ->
                            "ROLE_ADMIN".equals(a.getAuthority())
                                || "ROLE_MANAGER".equals(a.getAuthority())));
    if (!isOp) {
      throw new ForbiddenException("error.operator.forbidden");
    }
  }
}

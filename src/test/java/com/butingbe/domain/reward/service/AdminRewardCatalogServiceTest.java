package com.butingbe.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.dto.request.RewardCatalogCreateReqDto;
import com.butingbe.domain.reward.dto.request.RewardCatalogUpdateReqDto;
import com.butingbe.domain.reward.dto.response.RewardCatalogResDto;
import com.butingbe.domain.reward.dto.response.RewardGrantPageResDto;
import com.butingbe.domain.reward.entity.GrantReason;
import com.butingbe.domain.reward.entity.RewardCatalog;
import com.butingbe.domain.reward.entity.RewardGrant;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.repository.RewardCatalogRepository;
import com.butingbe.domain.reward.repository.RewardGrantRepository;
import com.butingbe.global.error.exception.DuplicateResourceException;
import com.butingbe.global.error.exception.ForbiddenException;
import com.butingbe.global.error.exception.ResourceNotFoundException;
import com.butingbe.global.error.exception.UnauthenticatedException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminRewardCatalogServiceTest {

  private static final UUID ADMIN_ID = UUID.randomUUID();
  private static final UUID REWARD_ID = UUID.randomUUID();

  @Mock private RewardCatalogRepository rewardCatalogRepository;
  @Mock private RewardGrantRepository rewardGrantRepository;
  @Mock private FileStorageService fileStorageService;

  private AdminRewardCatalogService service;
  private AuthenticatedUser adminUser;
  private AuthenticatedUser normalUser;

  @BeforeEach
  void setUp() {
    service =
        new AdminRewardCatalogService(
            rewardCatalogRepository, rewardGrantRepository, fileStorageService);

    adminUser =
        new AuthenticatedUser(
            ADMIN_ID,
            "admin@example.com",
            "admin",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    normalUser =
        new AuthenticatedUser(
            UUID.randomUUID(),
            "user@example.com",
            "user",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Test
  @DisplayName("비관리자가 카탈로그 생성 시 403 Forbidden이다")
  void createCatalogForbidden() {
    RewardCatalogCreateReqDto req =
        new RewardCatalogCreateReqDto(
            RewardType.POINT, "POINT_100", "100포인트", 100, null, 1000, 10, null, true);

    assertThatThrownBy(() -> service.createCatalog(normalUser, req))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("중복된 카탈로그 코드 등록 시 409 Conflict(DuplicateResourceException)이다")
  void createCatalogDuplicateCode() {
    RewardCatalogCreateReqDto req =
        new RewardCatalogCreateReqDto(
            RewardType.POINT, "POINT_100", "100포인트", 100, null, 1000, 10, null, true);
    when(rewardCatalogRepository.existsByCode("POINT_100")).thenReturn(true);

    assertThatThrownBy(() -> service.createCatalog(adminUser, req))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  @DisplayName("정상적인 파라미터로 신규 카탈로그를 생성한다")
  void createCatalogSuccess() {
    when(fileStorageService.getPresignedUrl("rewards/point100.png"))
        .thenReturn("https://s3.example.com/point100.png");
    RewardCatalogCreateReqDto req =
        new RewardCatalogCreateReqDto(
            RewardType.POINT,
            "POINT_100",
            "100포인트",
            100,
            "rewards/point100.png",
            1000,
            10,
            null,
            true);
    when(rewardCatalogRepository.existsByCode("POINT_100")).thenReturn(false);

    when(rewardCatalogRepository.save(any()))
        .thenAnswer(
            inv -> {
              RewardCatalog c = inv.getArgument(0);
              ReflectionTestUtils.setField(c, "id", REWARD_ID);
              ReflectionTestUtils.setField(c, "createdAt", LocalDateTime.now());
              ReflectionTestUtils.setField(c, "updatedAt", LocalDateTime.now());
              return c;
            });

    RewardCatalogResDto res = service.createCatalog(adminUser, req);

    assertThat(res.rewardId()).isEqualTo(REWARD_ID.toString());
    assertThat(res.code()).isEqualTo("POINT_100");
    assertThat(res.imageUrl()).isEqualTo("https://s3.example.com/point100.png");
  }

  @Test
  @DisplayName("보상 카탈로그 항목을 수정한다")
  void updateCatalogSuccess() {
    when(fileStorageService.getPresignedUrl("rewards/point100.png"))
        .thenReturn("https://s3.example.com/point100.png");
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.POINT)
            .code("POINT_100")
            .name("기존 명칭")
            .imageFileKey("rewards/point100.png")
            .stock(100)
            .build();
    ReflectionTestUtils.setField(catalog, "id", REWARD_ID);
    ReflectionTestUtils.setField(catalog, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(catalog, "updatedAt", LocalDateTime.now());

    when(rewardCatalogRepository.findById(REWARD_ID)).thenReturn(Optional.of(catalog));

    RewardCatalogUpdateReqDto req = new RewardCatalogUpdateReqDto("새 명칭", 50, 5, false);

    RewardCatalogResDto res = service.updateCatalog(adminUser, REWARD_ID, req);

    assertThat(res.name()).isEqualTo("새 명칭");
    assertThat(res.stock()).isEqualTo(50);
    assertThat(res.active()).isFalse();
    assertThat(res.imageUrl()).isEqualTo("https://s3.example.com/point100.png");
  }

  @Test
  @DisplayName("카탈로그 목록을 조건 검색한다")
  void getCatalogSuccess() {
    when(fileStorageService.getPresignedUrl("rewards/point100.png"))
        .thenReturn("https://s3.example.com/point100.png");
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.POINT)
            .code("POINT_100")
            .name("100포인트")
            .imageFileKey("rewards/point100.png")
            .build();
    ReflectionTestUtils.setField(catalog, "id", REWARD_ID);
    ReflectionTestUtils.setField(catalog, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(catalog, "updatedAt", LocalDateTime.now());

    when(rewardCatalogRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenAnswer(
            inv -> {
              Specification<RewardCatalog> spec = inv.getArgument(0);
              if (spec != null) {
                jakarta.persistence.criteria.Root<RewardCatalog> root =
                    mock(
                        jakarta.persistence.criteria.Root.class,
                        org.mockito.Mockito.RETURNS_DEEP_STUBS);
                jakarta.persistence.criteria.CriteriaQuery<?> cq =
                    mock(jakarta.persistence.criteria.CriteriaQuery.class);
                jakarta.persistence.criteria.CriteriaBuilder cb =
                    mock(
                        jakarta.persistence.criteria.CriteriaBuilder.class,
                        org.mockito.Mockito.RETURNS_DEEP_STUBS);
                spec.toPredicate(root, cq, cb);
              }
              return List.of(catalog);
            });

    List<RewardCatalogResDto> list = service.getCatalog(adminUser, RewardType.POINT, true);

    assertThat(list).hasSize(1);
    assertThat(list.get(0).code()).isEqualTo("POINT_100");
  }

  @Test
  @DisplayName("보상 발급 이력을 커서 페이징 조회한다")
  void getGrantsSuccess() {
    when(rewardCatalogRepository.existsById(REWARD_ID)).thenReturn(true);

    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.POINT)
            .code("POINT_100")
            .name("100포인트")
            .build();
    ReflectionTestUtils.setField(catalog, "id", REWARD_ID);

    RewardGrant grant =
        RewardGrant.builder()
            .userId(UUID.randomUUID())
            .reward(catalog)
            .grantReason(GrantReason.BASE)
            .build();
    ReflectionTestUtils.setField(grant, "id", UUID.randomUUID());

    when(rewardGrantRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(grant)));

    RewardGrantPageResDto res = service.getGrants(adminUser, REWARD_ID, null, 10);

    assertThat(res.items()).hasSize(1);
    assertThat(res.hasNext()).isFalse();
  }

  @Test
  @DisplayName("보상 카탈로그 목록을 필터와 함께 조회한다")
  void getCatalogWithFiltersSuccess() {
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.POINT)
            .code("POINT_100")
            .name("100포인트")
            .build();
    ReflectionTestUtils.setField(catalog, "id", REWARD_ID);
    ReflectionTestUtils.setField(catalog, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(catalog, "updatedAt", LocalDateTime.now());

    when(rewardCatalogRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of(catalog));

    List<RewardCatalogResDto> res = service.getCatalog(adminUser, RewardType.POINT, true);

    assertThat(res).hasSize(1);
    assertThat(res.get(0).rewardId()).isEqualTo(REWARD_ID.toString());
    assertThat(res.get(0).code()).isEqualTo("POINT_100");
  }

  @Test
  @DisplayName("존재하지 않는 카탈로그 수정 시 404이다")
  void updateCatalogNotFound() {
    when(rewardCatalogRepository.findById(REWARD_ID)).thenReturn(Optional.empty());
    RewardCatalogUpdateReqDto req = new RewardCatalogUpdateReqDto("새 명칭", 50, 5, false);

    assertThatThrownBy(() -> service.updateCatalog(adminUser, REWARD_ID, req))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("발급 이력 커서와 hasNext를 검증한다")
  void getGrantsWithCursorAndHasNext() {
    when(rewardCatalogRepository.existsById(REWARD_ID)).thenReturn(true);

    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.POINT)
            .code("POINT_100")
            .name("100포인트")
            .build();
    ReflectionTestUtils.setField(catalog, "id", REWARD_ID);

    RewardGrant g1 =
        RewardGrant.builder()
            .userId(UUID.randomUUID())
            .reward(catalog)
            .grantReason(GrantReason.BASE)
            .build();
    ReflectionTestUtils.setField(g1, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(g1, "grantedAt", OffsetDateTime.now());

    RewardGrant g2 =
        RewardGrant.builder()
            .userId(UUID.randomUUID())
            .reward(catalog)
            .grantReason(GrantReason.BASE)
            .build();
    ReflectionTestUtils.setField(g2, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(g2, "grantedAt", OffsetDateTime.now().minusMinutes(10));

    Page<RewardGrant> page = new PageImpl<>(List.of(g1, g2));
    when(rewardGrantRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenAnswer(
            inv -> {
              Specification<RewardGrant> spec = inv.getArgument(0);
              if (spec != null) {
                jakarta.persistence.criteria.Root<RewardGrant> root =
                    mock(
                        jakarta.persistence.criteria.Root.class,
                        org.mockito.Mockito.RETURNS_DEEP_STUBS);
                jakarta.persistence.criteria.CriteriaQuery<?> cq =
                    mock(jakarta.persistence.criteria.CriteriaQuery.class);
                jakarta.persistence.criteria.CriteriaBuilder cb =
                    mock(
                        jakarta.persistence.criteria.CriteriaBuilder.class,
                        org.mockito.Mockito.RETURNS_DEEP_STUBS);
                spec.toPredicate(root, cq, cb);
              }
              return page;
            });

    String cursor =
        java.util.Base64.getUrlEncoder()
            .encodeToString("2026-09-01T00:00:00Z_00000000-0000-0000-0000-000000000001".getBytes());

    RewardGrantPageResDto res = service.getGrants(adminUser, REWARD_ID, cursor, 1);

    assertThat(res.items()).hasSize(1);
    assertThat(res.hasNext()).isTrue();
    assertThat(res.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("존재하지 않는 카탈로그의 발급 이력 조회 시 404이다")
  void getGrantsCatalogNotFound() {
    when(rewardCatalogRepository.existsById(REWARD_ID)).thenReturn(false);

    assertThatThrownBy(() -> service.getGrants(adminUser, REWARD_ID, null, 10))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("발급 이력 조회 시 잘못된 커서이면 IllegalArgumentException을 던진다")
  void getGrantsInvalidCursor() {
    when(rewardCatalogRepository.existsById(REWARD_ID)).thenReturn(true);

    assertThatThrownBy(() -> service.getGrants(adminUser, REWARD_ID, "invalid_cursor", 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("관리자가 아닌 경우 403 Forbidden이다")
  void verifyOperatorForbidden() {
    AuthenticatedUser normalUser =
        new AuthenticatedUser(
            UUID.randomUUID(), "user", "user", List.of(new SimpleGrantedAuthority("ROLE_USER")));

    assertThatThrownBy(() -> service.getCatalog(normalUser, null, null))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("인증되지 않은 유저인 경우 401 Unauthenticated이다")
  void verifyOperatorUnauthenticated() {
    assertThatThrownBy(() -> service.getCatalog(null, null, null))
        .isInstanceOf(UnauthenticatedException.class);
  }
}

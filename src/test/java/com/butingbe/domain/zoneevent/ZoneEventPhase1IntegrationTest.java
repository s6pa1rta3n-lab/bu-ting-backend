package com.butingbe.domain.zoneevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.butingbe.domain.auth.security.AuthenticatedUser;
import com.butingbe.domain.chat.entity.ChatZone;
import com.butingbe.domain.file.dto.FileUploadResDto;
import com.butingbe.domain.file.service.FileStorageService;
import com.butingbe.domain.reward.dto.request.RewardCatalogCreateReqDto;
import com.butingbe.domain.reward.dto.response.PointLedgerPageResDto;
import com.butingbe.domain.reward.dto.response.UserRewardsResDto;
import com.butingbe.domain.reward.entity.RewardType;
import com.butingbe.domain.reward.service.AdminRewardCatalogService;
import com.butingbe.domain.reward.service.UserRewardService;
import com.butingbe.domain.user.entity.Name;
import com.butingbe.domain.user.entity.User;
import com.butingbe.domain.user.entity.UserRole;
import com.butingbe.domain.user.repository.UserRepository;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventCreateReqDto;
import com.butingbe.domain.zoneevent.dto.request.AdminZoneEventTargetReqDto;
import com.butingbe.domain.zoneevent.dto.request.ParticipationSubmitReqDto;
import com.butingbe.domain.zoneevent.dto.request.RewardSnapshotReqDto;
import com.butingbe.domain.zoneevent.dto.response.ParticipationResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventDetailResDto;
import com.butingbe.domain.zoneevent.dto.response.ZoneEventParticipationPageResDto;
import com.butingbe.domain.zoneevent.entity.ZoneEventTargetKind;
import com.butingbe.domain.zoneevent.entity.ZoneEventType;
import com.butingbe.domain.zoneevent.repository.ZoneEventTypeRepository;
import com.butingbe.domain.zoneevent.service.AdminZoneEventService;
import com.butingbe.domain.zoneevent.service.ZoneEventParticipationService;
import com.butingbe.domain.zoneevent.service.ZoneEventSubmitService;
import com.butingbe.global.error.exception.ConflictException;
import com.butingbe.support.AbstractContainerTest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Transactional
@Import(ZoneEventPhase1IntegrationTest.FileStorageTestConfig.class)
class ZoneEventPhase1IntegrationTest extends AbstractContainerTest {

  @TestConfiguration
  static class FileStorageTestConfig {
    @Bean
    @Primary
    FileStorageService fileStorageService() {
      return new FileStorageService() {
        @Override
        public FileUploadResDto upload(MultipartFile file) {
          throw new UnsupportedOperationException();
        }

        @Override
        public String getPresignedUrl(String fileKey) {
          return "https://signed.example.com/" + fileKey;
        }

        @Override
        public void delete(String fileKey) {}
      };
    }
  }

  @Autowired private AdminZoneEventService adminZoneEventService;
  @Autowired private AdminRewardCatalogService adminRewardCatalogService;
  @Autowired private ZoneEventParticipationService participationService;
  @Autowired private ZoneEventSubmitService submitService;
  @Autowired private UserRewardService userRewardService;
  @Autowired private UserRepository userRepository;
  @Autowired private ZoneEventTypeRepository zoneEventTypeRepository;

  @Autowired
  private com.butingbe.domain.file.repository.FileMetadataRepository fileMetadataRepository;

  private AuthenticatedUser adminUser;
  private AuthenticatedUser regularUser;
  private User dbUser;

  @BeforeEach
  void setUp() {
    User adminEntity =
        userRepository.save(
            User.builder()
                .email("admin@buting.test")
                .provider("google")
                .providerId("admin-sub-1")
                .name(new Name("Admin", "Kim"))
                .nickname("SuperAdmin")
                .role(UserRole.ADMIN)
                .build());

    adminUser =
        new AuthenticatedUser(
            adminEntity.getId(),
            adminEntity.getEmail(),
            adminEntity.getNickname(),
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    dbUser =
        userRepository.save(
            User.builder()
                .email("traveler@buting.test")
                .provider("google")
                .providerId("traveler-sub-1")
                .name(new Name("Traveler", "Lee"))
                .nickname("BusanExplorer")
                .role(UserRole.USER)
                .build());

    regularUser = AuthenticatedUser.from(dbUser);

    if (!zoneEventTypeRepository.existsById("PLACE_AUTH")) {
      zoneEventTypeRepository.save(
          ZoneEventType.builder()
              .typeCode("PLACE_AUTH")
              .name("장소 인증")
              .description("지정된 장소에서 사진 촬영 및 GPS 인증")
              .requiresUpload(true)
              .build());
    }

    fileMetadataRepository.save(
        com.butingbe.domain.file.entity.FileMetadata.builder()
            .objectKey("events/user-photo-1.jpg")
            .originalFileName("photo.jpg")
            .contentType("image/jpeg")
            .mediaType("IMAGE")
            .fileSize(1024L)
            .bucket("test-bucket")
            .build());
  }

  @Test
  @DisplayName("구역 이벤트 Phase 1 핵심 라이프사이클 E2E 통합 검증 (생성 -> 활성화 -> 참여 -> 인증/보상 -> 조회)")
  void fullZoneEventPhase1Lifecycle() {
    adminRewardCatalogService.createCatalog(
        adminUser,
        new RewardCatalogCreateReqDto(
            RewardType.BADGE,
            "SPOT_GWANGAN_BRIDGE_E2E",
            "광안대교 탐험가 배지",
            null,
            null,
            null,
            null,
            null,
            null));

    adminRewardCatalogService.createCatalog(
        adminUser,
        new RewardCatalogCreateReqDto(
            RewardType.POINT, "POINT_BASE", "기본 포인트", 50, null, null, null, null, null));

    AdminZoneEventTargetReqDto targetReq =
        new AdminZoneEventTargetReqDto(
            ZoneEventTargetKind.PLACE,
            null,
            "광안리 해수욕장",
            "광안대교가 보이도록 사진을 촬영해주세요.",
            null,
            35.1532,
            129.1186,
            100);

    RewardSnapshotReqDto baseReward =
        new RewardSnapshotReqDto(50, "SPOT_GWANGAN_BRIDGE_E2E", null, null);

    AdminZoneEventCreateReqDto createReq =
        new AdminZoneEventCreateReqDto(
            ChatZone.SUYEONG_NAMGU.name(),
            "PLACE_AUTH",
            "광안리 일몰 챌린지",
            "일몰 시간에 맞춰 광안대교를 인증해보세요.",
            OffsetDateTime.now().minusMinutes(5),
            120,
            targetReq,
            baseReward,
            null,
            1);

    ZoneEventDetailResDto createdEvent = adminZoneEventService.createEvent(adminUser, createReq);
    UUID eventId = UUID.fromString(createdEvent.eventId());
    assertThat(createdEvent.status()).isEqualTo("SCHEDULED");

    ZoneEventDetailResDto activatedEvent = adminZoneEventService.activateEvent(adminUser, eventId);
    assertThat(activatedEvent.status()).isEqualTo("ACTIVE");

    ParticipationResDto joined = participationService.join(regularUser, eventId, 35.1533, 129.1185);
    UUID participationId = UUID.fromString(joined.participationId());
    assertThat(joined.status()).isEqualTo("JOINED");

    ParticipationSubmitReqDto submitReq =
        new ParticipationSubmitReqDto(
            "events/user-photo-1.jpg", "광안리 인증 완료했습니다!", 35.1532, 129.1186, OffsetDateTime.now());

    com.butingbe.domain.zoneevent.dto.response.SubmitResultResDto submitted =
        submitService.submit(regularUser, eventId, participationId, submitReq);
    assertThat(submitted.participation().status()).isEqualTo("SUCCESS");
    assertThat(submitted.participation().success()).isTrue();
    assertThat(submitted.pointBalance()).isEqualTo(50);

    UserRewardsResDto myRewards = userRewardService.getMyRewards(regularUser);
    assertThat(myRewards.pointBalance()).isEqualTo(50L);
    assertThat(myRewards.badges())
        .anyMatch(
            group ->
                group.zoneId().equals(ChatZone.SUYEONG_NAMGU.name())
                    && group.badges().stream()
                        .anyMatch(b -> b.code().equals("SPOT_GWANGAN_BRIDGE_E2E")));

    PointLedgerPageResDto ledger = userRewardService.getPointLedger(regularUser, null, 10);
    assertThat(ledger.items()).hasSize(1);
    assertThat(ledger.items().get(0).amount()).isEqualTo(50);
    assertThat(ledger.items().get(0).reason()).isEqualTo("BASE");

    ZoneEventParticipationPageResDto myHistory =
        participationService.getMyParticipations(
            regularUser, null, 10, null, null, null, null, null);
    assertThat(myHistory.items()).hasSize(1);
    assertThat(myHistory.items().get(0).status()).isEqualTo("SUCCESS");
    assertThat(myHistory.items().get(0).rewards()).isNotEmpty();

    assertThatThrownBy(() -> participationService.cancel(regularUser, eventId, participationId))
        .isInstanceOf(ConflictException.class);

    ZoneEventDetailResDto closedEvent = adminZoneEventService.closeEvent(adminUser, eventId);
    assertThat(closedEvent.status()).isEqualTo("CLOSED");
    assertThat(closedEvent.successCount()).isEqualTo(1L);
  }
}

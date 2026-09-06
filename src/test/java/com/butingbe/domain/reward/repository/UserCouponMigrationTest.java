package com.butingbe.domain.reward.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/** V38 마이그레이션이 생성하는 user_coupon 테이블과 제약을 실제 PostgreSQL 컨테이너로 검증한다. */
class UserCouponMigrationTest {

  @Test
  @DisplayName("V38 마이그레이션은 user_coupon 테이블과 상태 CHECK, 쿠폰 코드 UK, 지급 ID UK 제약을 생성한다")
  void migrateUserCoupon() throws Exception {
    try (PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("buting_coupon_test")
            .withUsername("test_user")
            .withPassword("test_password")) {
      postgres.start();

      Flyway.configure()
          .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
          .locations("classpath:db/migration")
          .load()
          .migrate();

      try (Connection connection =
          DriverManager.getConnection(
              postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

        UUID userId = insertUser(connection);
        UUID rewardId = insertCatalog(connection, "COUPON", "COUPON_COFFEE");
        UUID grantId1 = insertGrant(connection, userId, rewardId);
        UUID grantId2 = insertGrant(connection, userId, rewardId);

        insertCoupon(connection, userId, rewardId, grantId1, "CODE-AAA", "ISSUED");
        assertThat(count(connection, "user_coupon")).isEqualTo(1);

        assertThat(
                catchThrowable(
                    () ->
                        insertCoupon(
                            connection, userId, rewardId, grantId2, "CODE-BBB", "INVALID_STATUS")))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("ck_user_coupon_status");

        assertThat(
                catchThrowable(
                    () ->
                        insertCoupon(connection, userId, rewardId, grantId2, "CODE-AAA", "ISSUED")))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("uk_user_coupon_code");

        assertThat(
                catchThrowable(
                    () ->
                        insertCoupon(connection, userId, rewardId, grantId1, "CODE-CCC", "ISSUED")))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("uk_user_coupon_grant");
      }
    }
  }

  private void insertCoupon(
      Connection connection, UUID userId, UUID rewardId, UUID grantId, String code, String status)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO user_coupon (
              coupon_id, user_id, reward_id, grant_id, coupon_code, status, issued_at)
            VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, now())
            """)) {
      statement.setObject(1, userId);
      statement.setObject(2, rewardId);
      statement.setObject(3, grantId);
      statement.setString(4, code);
      statement.setString(5, status);
      statement.executeUpdate();
    }
  }

  private UUID insertGrant(Connection connection, UUID userId, UUID rewardId) throws SQLException {
    UUID grantId = UUID.randomUUID();
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO reward_grant (grant_id, user_id, reward_id, grant_reason, granted_at)
            VALUES (?, ?, ?, 'TOP_LIKE', now())
            """)) {
      statement.setObject(1, grantId);
      statement.setObject(2, userId);
      statement.setObject(3, rewardId);
      statement.executeUpdate();
    }
    return grantId;
  }

  private UUID insertCatalog(Connection connection, String rewardType, String code)
      throws SQLException {
    UUID rewardId = UUID.randomUUID();
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO reward_catalog (reward_id, reward_type, code, name, active)
            VALUES (?, ?, ?, '테스트', TRUE)
            """)) {
      statement.setObject(1, rewardId);
      statement.setString(2, rewardType);
      statement.setString(3, code);
      statement.executeUpdate();
    }
    return rewardId;
  }

  private UUID insertUser(Connection connection) throws SQLException {
    UUID userId = UUID.randomUUID();
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO users (
              id, email, last_name, first_name, nickname, role, created_at, updated_at)
            VALUES (?, ?, '홍', '길동', 'tester', 'USER', now(), now())
            """)) {
      statement.setObject(1, userId);
      statement.setString(2, "coupon-" + userId + "@example.com");
      statement.executeUpdate();
    }
    return userId;
  }

  private long count(Connection connection, String table) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("SELECT count(*) FROM " + table)) {
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1);
      }
    }
  }
}

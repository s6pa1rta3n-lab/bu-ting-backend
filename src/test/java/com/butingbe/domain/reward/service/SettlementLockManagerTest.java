package com.butingbe.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettlementLockManagerTest {

  private final SettlementLockManager lockManager = new SettlementLockManager();

  @Test
  @DisplayName("null targetId인 경우 락 획득 없이 작업을 즉시 실행한다")
  void executeWithNullTargetId() {
    String result = lockManager.executeWithLock(null, () -> "executed");
    assertThat(result).isEqualTo("executed");
  }

  @Test
  @DisplayName("유효한 targetId에 대해 작업을 성공적으로 실행하고 결과를 반환한다")
  void executeWithLockSuccess() {
    UUID targetId = UUID.randomUUID();
    int result = lockManager.executeWithLock(targetId, () -> 42);
    assertThat(result).isEqualTo(42);
  }

  @Test
  @DisplayName("작업 도중 예외가 발생해도 락이 안전하게 해제된다")
  void executeWithLockException() {
    UUID targetId = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                lockManager.executeWithLock(
                    targetId,
                    () -> {
                      throw new IllegalStateException("error");
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("error");

    String result = lockManager.executeWithLock(targetId, () -> "recovered");
    assertThat(result).isEqualTo("recovered");
  }
}

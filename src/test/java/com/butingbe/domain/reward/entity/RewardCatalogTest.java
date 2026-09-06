package com.butingbe.domain.reward.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.butingbe.global.error.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RewardCatalogTest {

  @Test
  @DisplayName("재고가 무제한(null)인 경우 decreaseStock 호출 시 예외 없이 감소하지 않는다")
  void decreaseStockNullStock() {
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.POINT)
            .code("P_100")
            .name("100P")
            .stock(null)
            .build();

    catalog.decreaseStock(5);
    assertThat(catalog.getStock()).isNull();
  }

  @Test
  @DisplayName("수량이 0 이하이면 재고가 감소하지 않는다")
  void decreaseStockZeroOrNegative() {
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN")
            .name("쿠폰")
            .stock(10)
            .build();

    catalog.decreaseStock(0);
    catalog.decreaseStock(-1);
    assertThat(catalog.getStock()).isEqualTo(10);
  }

  @Test
  @DisplayName("보유 재고보다 많은 수량을 차감하려 하면 ConflictException이 발생한다")
  void decreaseStockInsufficient() {
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN")
            .name("쿠폰")
            .stock(3)
            .build();

    assertThatThrownBy(() -> catalog.decreaseStock(5))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.reward.out_of_stock");
  }

  @Test
  @DisplayName("충분한 재고가 있으면 정상적으로 차감된다")
  void decreaseStockSuccess() {
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN")
            .name("쿠폰")
            .stock(10)
            .build();

    catalog.decreaseStock(3);
    assertThat(catalog.getStock()).isEqualTo(7);
  }

  @Test
  @DisplayName("increaseStock은 무제한(null)인 경우 아무 동작도 하지 않는다")
  void increaseStockNull() {
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN")
            .name("쿠폰")
            .stock(null)
            .build();

    catalog.increaseStock(5);
    assertThat(catalog.getStock()).isNull();
  }

  @Test
  @DisplayName("increaseStock은 수량이 양수일 때 정상적으로 증가한다")
  void increaseStockSuccess() {
    RewardCatalog catalog =
        RewardCatalog.builder()
            .rewardType(RewardType.COUPON)
            .code("CPN")
            .name("쿠폰")
            .stock(5)
            .build();

    catalog.increaseStock(0);
    catalog.increaseStock(-2);
    assertThat(catalog.getStock()).isEqualTo(5);

    catalog.increaseStock(3);
    assertThat(catalog.getStock()).isEqualTo(8);
  }
}

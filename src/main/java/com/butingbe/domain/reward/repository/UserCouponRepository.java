package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.CouponStatus;
import com.butingbe.domain.reward.entity.UserCoupon;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, UUID> {

  List<UserCoupon> findByUserIdOrderByIssuedAtDesc(UUID userId);

  List<UserCoupon> findByUserIdAndStatusOrderByIssuedAtDesc(UUID userId, CouponStatus status);

  Optional<UserCoupon> findByIdAndUserId(UUID id, UUID userId);

  Optional<UserCoupon> findByGrantId(UUID grantId);

  List<UserCoupon> findByGrantIdIn(Collection<UUID> grantIds);

  boolean existsByCouponCode(String couponCode);
}

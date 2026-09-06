package com.butingbe.domain.reward.repository;

import com.butingbe.domain.reward.entity.UserPointBalance;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저 포인트 잔액 저장소 인터페이스. */
public interface UserPointBalanceRepository extends JpaRepository<UserPointBalance, UUID> {}

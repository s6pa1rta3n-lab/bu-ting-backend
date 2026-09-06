package com.butingbe.domain.reward.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** 회차·이벤트 단위 정산 락 관리자 (BR-12). 동일 대상에 대한 동시 정산 실행을 방지한다. */
@Component
public class SettlementLockManager {

  private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

  /** 대상 ID에 대한 락을 획득하고 작업을 실행한다. */
  public <T> T executeWithLock(UUID targetId, Supplier<T> action) {
    if (targetId == null) {
      return action.get();
    }
    ReentrantLock lock = locks.computeIfAbsent(targetId, k -> new ReentrantLock());
    lock.lock();
    try {
      return action.get();
    } finally {
      lock.unlock();
      if (!lock.hasQueuedThreads()) {
        locks.remove(targetId, lock);
      }
    }
  }
}

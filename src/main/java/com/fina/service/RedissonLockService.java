package com.fina.service;

import com.fina.exception.ConflictException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class RedissonLockService implements LockService {

    private static final long WAIT_SECONDS = 5;
    private static final long LEASE_SECONDS = 15;

    private final RedissonClient redissonClient;

    public RedissonLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        acquire(lock);
        try {
            return action.get();
        } finally {
            release(lock);
        }
    }

    @Override
    public <T> T withLocks(Collection<String> keys, Supplier<T> action) {
        List<String> orderedKeys = keys.stream().distinct().sorted().toList();
        if (orderedKeys.size() <= 1) {
            return withLock(orderedKeys.isEmpty() ? "fina:lock:noop" : orderedKeys.getFirst(), action);
        }

        RLock multiLock = redissonClient.getMultiLock(
                orderedKeys.stream().map(redissonClient::getLock).toArray(RLock[]::new)
        );
        acquire(multiLock);
        try {
            return action.get();
        } finally {
            release(multiLock);
        }
    }

    private void acquire(RLock lock) {
        try {
            if (!lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS)) {
                throw new ConflictException("Resource is busy, please retry");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ConflictException("Interrupted while waiting for resource lock");
        }
    }

    private void release(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}

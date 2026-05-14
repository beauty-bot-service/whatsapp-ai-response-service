package com.beautybot.whatsappairesponseservice.conversation.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
@Service
public class ConversationProcessingLockService {

    private final Map<String, LockHolder> locks = new ConcurrentHashMap<>();

    public <T> T executeLocked(String phoneNumber, Supplier<T> operation) {
        String key = normalize(phoneNumber);
        LockHolder holder = locks.compute(key, (ignored, current) -> {
            LockHolder resolved = current == null ? new LockHolder() : current;
            resolved.incrementUsers();
            return resolved;
        });

        ReentrantLock lock = holder.lock();
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
            releaseHolder(key, holder);
        }
    }

    public void executeLocked(String phoneNumber, Runnable operation) {
        executeLocked(phoneNumber, () -> {
            operation.run();
            return null;
        });
    }

    private String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "unknown";
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        return digits.isBlank() ? phoneNumber.trim() : digits;
    }

    private void releaseHolder(String key, LockHolder holder) {
        locks.computeIfPresent(key, (ignored, current) -> {
            if (current != holder) {
                return current;
            }

            int remainingUsers = current.decrementUsers();
            if (remainingUsers == 0
                    && !current.lock().isLocked()
                    && !current.lock().hasQueuedThreads()) {
                return null;
            }
            return current;
        });
    }

    private static final class LockHolder {
        private final ReentrantLock lock = new ReentrantLock(true);
        private final AtomicInteger users = new AtomicInteger(0);

        private ReentrantLock lock() {
            return lock;
        }

        private void incrementUsers() {
            users.incrementAndGet();
        }

        private int decrementUsers() {
            return users.decrementAndGet();
        }
    }
}

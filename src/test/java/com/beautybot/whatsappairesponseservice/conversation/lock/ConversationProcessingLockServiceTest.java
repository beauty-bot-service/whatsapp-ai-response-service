package com.beautybot.whatsappairesponseservice.conversation.lock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationProcessingLockServiceTest {

    @Test
    void serializesOperationsForSamePhoneNumber() throws Exception {
        ConversationProcessingLockService service = new ConversationProcessingLockService();
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            tasks.add(() -> {
                service.executeLocked("+54 9 11 1234-5678", () -> {
                    int running = concurrent.incrementAndGet();
                    maxConcurrent.updateAndGet(previous -> Math.max(previous, running));
                    sleep(10);
                    concurrent.decrementAndGet();
                });
                return null;
            });
        }

        runTasks(tasks, 8);
        assertThat(maxConcurrent.get()).isEqualTo(1);
    }

    @Test
    void allowsParallelOperationsForDifferentPhoneNumbers() throws Exception {
        ConversationProcessingLockService service = new ConversationProcessingLockService();
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        List<Callable<Void>> tasks = List.of(
                () -> {
                    service.executeLocked("5491111111111", () -> {
                        int running = concurrent.incrementAndGet();
                        maxConcurrent.updateAndGet(previous -> Math.max(previous, running));
                        sleep(60);
                        concurrent.decrementAndGet();
                    });
                    return null;
                },
                () -> {
                    service.executeLocked("5491222222222", () -> {
                        int running = concurrent.incrementAndGet();
                        maxConcurrent.updateAndGet(previous -> Math.max(previous, running));
                        sleep(60);
                        concurrent.decrementAndGet();
                    });
                    return null;
                }
        );

        runTasks(tasks, 2);
        assertThat(maxConcurrent.get()).isGreaterThanOrEqualTo(2);
    }

    private void runTasks(List<Callable<Void>> tasks, int poolSize) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdown();
            boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
            assertThat(terminated).isTrue();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Thread interrupted during test sleep", ex);
        }
    }
}

package edu.eci.arsw.concurrency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests enfocados en verificar el comportamiento del PauseController
 * en escenarios de concurrencia.
 */
class PauseControllerTest {

    @Test
    @DisplayName("PauseController debe permitir pausar y reanudar hilos correctamente")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldPauseAndResumeThreadsCorrectly() throws InterruptedException {
        var controller = new PauseController();
        controller.setTotalThreads(2);

        var counter = new AtomicInteger(0);
        var startLatch = new CountDownLatch(2);

        Thread worker1 = new Thread(() -> {
            try {
                startLatch.countDown();
                for (int i = 0; i < 100; i++) {
                    controller.awaitIfPaused();
                    counter.incrementAndGet();
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread worker2 = new Thread(() -> {
            try {
                startLatch.countDown();
                for (int i = 0; i < 100; i++) {
                    controller.awaitIfPaused();
                    counter.incrementAndGet();
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker1.start();
        worker2.start();

        startLatch.await();
        Thread.sleep(50);

        controller.pause();
        assertTrue(controller.paused());

        controller.waitUntilAllPaused();

        int countWhenPaused = counter.get();
        Thread.sleep(100);
        assertEquals(countWhenPaused, counter.get(),
                "El contador no debería cambiar mientras está pausado");

        controller.resume();
        assertFalse(controller.paused());

        Thread.sleep(50);
        assertTrue(counter.get() > countWhenPaused,
                "El contador debería incrementar después de reanudar");

        worker1.join();
        worker2.join();
    }

    @Test
    @DisplayName("PauseController debe manejar correctamente múltiples pausas y reanudaciones")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldHandleMultiplePauseResumeCycles() throws InterruptedException {
        var controller = new PauseController();
        controller.setTotalThreads(1);

        var executionSteps = new AtomicInteger(0);

        Thread worker = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    controller.awaitIfPaused();
                    executionSteps.incrementAndGet();
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        Thread.sleep(30);

        for (int cycle = 0; cycle < 3; cycle++) {
            controller.pause();
            controller.waitUntilAllPaused();

            int stepsBefore = executionSteps.get();
            Thread.sleep(50);
            assertEquals(stepsBefore, executionSteps.get(),
                    "No debería ejecutarse mientras está pausado");

            controller.resume();
            Thread.sleep(30);
        }

        worker.join();
        assertTrue(executionSteps.get() > 0, "Debería haber ejecutado algunos pasos");
    }
}

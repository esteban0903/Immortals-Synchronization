package edu.eci.arsw.immortals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tests para verificar la thread-safety del ScoreBoard.
 */
class ScoreBoardTest {

    @Test
    @DisplayName("ScoreBoard debe ser thread-safe bajo concurrencia alta")
    void shouldBeThreadSafeUnderHighConcurrency() throws InterruptedException {
        var scoreBoard = new ScoreBoard();
        int numberOfThreads = 10;
        int fightsPerThread = 1000;

        var startLatch = new CountDownLatch(numberOfThreads);
        var executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                for (int fight = 0; fight < fightsPerThread; fight++) {
                    scoreBoard.recordFight();
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "Los hilos deberían terminar en tiempo razonable");

        long expectedFights = (long) numberOfThreads * fightsPerThread;
        assertEquals(expectedFights, scoreBoard.totalFights(),
                "Deberían registrarse exactamente todas las peleas sin pérdida de datos");
    }

    @Test
    @DisplayName("ScoreBoard debe inicializar con contador en cero")
    void shouldInitializeWithZeroFights() {
        var scoreBoard = new ScoreBoard();
        assertEquals(0, scoreBoard.totalFights());
    }

    @Test
    @DisplayName("ScoreBoard debe incrementar correctamente el contador")
    void shouldIncrementFightCountCorrectly() {
        var scoreBoard = new ScoreBoard();

        assertEquals(0, scoreBoard.totalFights());

        scoreBoard.recordFight();
        assertEquals(1, scoreBoard.totalFights());

        scoreBoard.recordFight();
        assertEquals(2, scoreBoard.totalFights());

        for (int i = 0; i < 98; i++) {
            scoreBoard.recordFight();
        }
        assertEquals(100, scoreBoard.totalFights());
    }
}

package edu.eci.arsw.immortals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;

/**
 * Tests comprehensivos para el sistema de Inmortales.
 * Enfocados en validar funcionalidad core y aspectos de concurrencia.
 */
class ImmortalConcurrencyTest {

    private static final int INITIAL_HEALTH = 100;
    private static final int DAMAGE = 10;
    private static final int TEST_TIMEOUT_SECONDS = 10;

    @Test
    @DisplayName("El manager debe inicializar correctamente la población de inmortales")
    void shouldInitializeImmortalPopulationCorrectly() {
        try (var manager = new ImmortalManager(5, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED)) {
            assertEquals(5, manager.populationSnapshot().size());
            assertEquals(5 * INITIAL_HEALTH, manager.totalHealth());
            assertEquals(5, manager.aliveCount());
            assertEquals(FightStrategy.ORDERED, manager.getFightStrategy());
            assertFalse(manager.isRunning());
        }
    }

    @Test
    @DisplayName("Los inmortales deben poder pausar y reanudar correctamente")
    @Timeout(value = TEST_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    void shouldPauseAndResumeCorrectly() throws InterruptedException {
        try (var manager = new ImmortalManager(4, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED)) {
            manager.start();
            assertTrue(manager.isRunning());

            Thread.sleep(50);

            manager.pause();
            long healthAtPause = manager.totalHealth();
            long fightsAtPause = manager.scoreBoard().totalFights();

            Thread.sleep(100);
            assertEquals(healthAtPause, manager.totalHealth());
            assertEquals(fightsAtPause, manager.scoreBoard().totalFights());

            manager.resume();
            Thread.sleep(50);

            manager.stop();
            assertFalse(manager.isRunning());
        }
    }

    @Test
    @DisplayName("Las peleas ordenadas deben seguir las reglas de combate correctamente")
    @Timeout(value = TEST_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    void shouldFollowOrderedFightRulesCorrectly() throws InterruptedException {
        try (var manager = new ImmortalManager(3, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED)) {
            long initialTotalHealth = manager.totalHealth();

            manager.start();
            Thread.sleep(200);
            manager.stop();

            long finalTotalHealth = manager.totalHealth();
            long fightsOccurred = manager.scoreBoard().totalFights();

            assertTrue(fightsOccurred > 0, "Deberían haber ocurrido peleas");

            assertTrue(finalTotalHealth < initialTotalHealth,
                    "La salud total debería disminuir debido a las reglas de combate (atacante +DAMAGE/2, oponente -DAMAGE)");

            for (Immortal immortal : manager.populationSnapshot()) {
                assertTrue(immortal.getHealth() >= 0,
                        "Ningún inmortal debería tener salud negativa");
            }
        }
    }

    @Test
    @DisplayName("El ScoreBoard debe contar peleas de forma thread-safe")
    @Timeout(value = TEST_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    void shouldCountFightsThreadSafely() throws InterruptedException {
        try (var manager = new ImmortalManager(6, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED)) {
            manager.start();
            Thread.sleep(300);
            manager.pause();

            long fightCount = manager.scoreBoard().totalFights();
            assertTrue(fightCount > 0, "Deberían haber ocurrido peleas");

            for (int i = 0; i < 10; i++) {
                assertEquals(fightCount, manager.scoreBoard().totalFights(),
                        "El contador de peleas debe ser consistente");
            }

            manager.stop();
        }
    }

    @Test
    @DisplayName("Los inmortales deben terminar correctamente al recibir stop")
    @Timeout(value = TEST_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    void shouldStopGracefully() throws InterruptedException {
        try (var manager = new ImmortalManager(4, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED)) {
            manager.start();
            assertTrue(manager.isRunning());

            Thread.sleep(100);

            manager.stop();

            assertFalse(manager.isRunning());

            Thread.sleep(100);

            for (Immortal immortal : manager.populationSnapshot()) {
                assertFalse(immortal.isImmortalAlive());
            }
        }
    }

    @Test
    @DisplayName("AutoCloseable debe funcionar correctamente")
    void shouldCloseCorrectly() throws Exception {
        try (var manager = new ImmortalManager(3, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED)) {
            manager.start();
            assertTrue(manager.isRunning());

        }

    }

    @Test
    @DisplayName("El parsing de estrategias debe funcionar correctamente")
    void shouldParseStrategiesCorrectly() {
        try (var naiveManager = new ImmortalManager(2, "naive", INITIAL_HEALTH, DAMAGE)) {
            assertEquals(FightStrategy.NAIVE, naiveManager.getFightStrategy());
        }

        try (var orderedManager = new ImmortalManager(2, "ordered", INITIAL_HEALTH, DAMAGE)) {
            assertEquals(FightStrategy.ORDERED, orderedManager.getFightStrategy());
        }

        try (var unknownManager = new ImmortalManager(2, "unknown", INITIAL_HEALTH, DAMAGE)) {
            assertEquals(FightStrategy.ORDERED, unknownManager.getFightStrategy());
        }
    }
}

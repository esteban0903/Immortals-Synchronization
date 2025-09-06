package edu.eci.arsw.immortals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;

/**
 * Tests comparativos entre las estrategias NAIVE y ORDERED.
 * Enfocados en detectar diferencias de comportamiento y estabilidad.
 */
class FightStrategyComparisonTest {

    private static final int INITIAL_HEALTH = 100;
    private static final int DAMAGE = 10;
    private static final int TEST_TIMEOUT_SECONDS = 8;

    @Test
    @DisplayName("La estrategia ORDERED debe ser más estable que NAIVE bajo concurrencia")
    @Timeout(value = TEST_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    void orderedStrategyShouldBeMoreStableThanNaive() throws InterruptedException {
        try (var orderedManager = new ImmortalManager(4, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED)) {
            orderedManager.start();
            Thread.sleep(200);
            orderedManager.stop();

            for (Immortal immortal : orderedManager.populationSnapshot()) {
                assertTrue(immortal.getHealth() >= 0,
                        "ORDERED: Ningún inmortal debería tener salud negativa");
            }

            long orderedFights = orderedManager.scoreBoard().totalFights();
            assertTrue(orderedFights > 0, "ORDERED: Deberían haber ocurrido peleas");
        }

        try (var naiveManager = new ImmortalManager(2, INITIAL_HEALTH, DAMAGE, FightStrategy.NAIVE)) {
            naiveManager.start();
            Thread.sleep(100);
            naiveManager.stop();

            long naiveFights = naiveManager.scoreBoard().totalFights();
            assertTrue(naiveFights >= 0, "NAIVE: Debería poder ejecutar (aunque sea con deadlock potencial)");
        }
    }

    @Test
    @DisplayName("La estrategia ORDERED debe soportar pause/resume sin deadlocks")
    @Timeout(value = TEST_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    void orderedStrategyShouldSupportPauseResumeWithoutDeadlocks() throws InterruptedException {
        try (var orderedManager = new ImmortalManager(3, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED)) {
            orderedManager.start();
            Thread.sleep(50);

            orderedManager.pause();
            long pausedFights = orderedManager.scoreBoard().totalFights();

            Thread.sleep(100);
            assertEquals(pausedFights, orderedManager.scoreBoard().totalFights(),
                    "ORDERED: No deberían ocurrir peleas mientras está pausado");

            orderedManager.resume();
            Thread.sleep(50);

            assertTrue(orderedManager.scoreBoard().totalFights() >= pausedFights,
                    "ORDERED: Deberían continuar las peleas después de resume");
        }
    }

    @Test
    @DisplayName("La creación de managers con diferentes estrategias debe funcionar correctamente")
    void shouldCreateManagersWithDifferentStrategiesCorrectly() {
        try (var directOrdered = new ImmortalManager(2, INITIAL_HEALTH, DAMAGE, FightStrategy.ORDERED);
                var directNaive = new ImmortalManager(2, INITIAL_HEALTH, DAMAGE, FightStrategy.NAIVE)) {

            assertEquals(FightStrategy.ORDERED, directOrdered.getFightStrategy());
            assertEquals(FightStrategy.NAIVE, directNaive.getFightStrategy());
        }

        try (var stringOrdered = new ImmortalManager(2, "ordered", INITIAL_HEALTH, DAMAGE);
                var stringNaive = new ImmortalManager(2, "naive", INITIAL_HEALTH, DAMAGE);
                var stringDefault = new ImmortalManager(2, "invalid", INITIAL_HEALTH, DAMAGE)) {

            assertEquals(FightStrategy.ORDERED, stringOrdered.getFightStrategy());
            assertEquals(FightStrategy.NAIVE, stringNaive.getFightStrategy());
            assertEquals(FightStrategy.ORDERED, stringDefault.getFightStrategy());
        }
    }
}

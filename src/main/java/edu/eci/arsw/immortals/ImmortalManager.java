package edu.eci.arsw.immortals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.eci.arsw.concurrency.PauseController;

public class ImmortalManager implements AutoCloseable {
  private final ConcurrentLinkedQueue<Immortal> population;
  private final List<Future<?>> futures = new ArrayList<>();
  private final PauseController pauseController;
  private final ScoreBoard scoreBoard;
  private ExecutorService executorService;
  private final FightStrategy fightStrategy;

  private final int initialHealth;
  private final int damage;

  /*
   * Constructor actualizado para incluir la estrategia de pelea.
   * - fightStrategy: Estrategia de pelea (NAIVE o ORDERED)
   */
  public ImmortalManager(int immortalsCount, int health, int damage,
      FightStrategy fightStrategy) {
    this.population = new ConcurrentLinkedQueue<>();
    this.scoreBoard = new ScoreBoard();
    this.pauseController = new PauseController();
    this.fightStrategy = fightStrategy;
    this.initialHealth = health;
    this.damage = damage;

    initializeImmortals(immortalsCount, health, damage);
    pauseController.setTotalThreads(population.size());
  }

  public ImmortalManager(int immortalsCount, String fightMode, int health, int damage) {
    this(immortalsCount, health, damage, parseFightStrategy(fightMode));
  }

  private static FightStrategy parseFightStrategy(String strategy) {
    return switch (strategy.toLowerCase()) {
      case "naive" -> FightStrategy.NAIVE;
      case "ordered" -> FightStrategy.ORDERED;
      default -> FightStrategy.ORDERED;
    };
  }

  private void initializeImmortals(int count, int health, int damage) {
    for (int i = 0; i < count; i++) {
      String name = String.format("Immortal_%d", i);
      Immortal immortal = new Immortal(name, health, damage, population,
          scoreBoard, pauseController,
          fightStrategy); 
      population.add(immortal);
    }
  }

  public synchronized void start() {
    if (executorService != null)
      stop();
    executorService = Executors.newVirtualThreadPerTaskExecutor();
    for (Immortal im : population) {
      futures.add(executorService.submit(im));
    }
  }

  public void pause() throws InterruptedException {
    pauseController.pause();
    pauseController.waitUntilAllPaused();
  }

  public void resume() {
    pauseController.resume();
  }

public void stop() {
    for (Immortal im : population) {
        im.stopImmortal();
    }

    if (pauseController.paused()) {
        pauseController.resume();
    }

    if (executorService != null) {
        executorService.shutdown(); 
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    population.clear();
}

  public int aliveCount() {
    int c = 0;
    for (Immortal im : population)
      if (im.isImmortalAlive())
        c++;
    return c;
  }

  public long totalHealth() {
    long sum = 0;
    for (Immortal im : population)
      sum += im.getHealth();
    return sum;
  }

  public List<Immortal> populationSnapshot() {
    return Collections.unmodifiableList(new ArrayList<>(population));
  }

  public FightStrategy getFightStrategy() {
    return fightStrategy;
  }

  public int getInitialHealth() {
    return initialHealth;
  }

  public int getDamage() {
    return damage;
  }

  public boolean isRunning() {
    return executorService != null && !executorService.isShutdown();
  }

  public ScoreBoard scoreBoard() {
    return scoreBoard;
  }

  public PauseController controller() {
    return pauseController;
  }

  @Override
  public void close() {
    stop();
  }
}

package edu.eci.arsw.immortals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.eci.arsw.concurrency.PauseController;

public class ImmortalManager implements AutoCloseable {
  private final List<Immortal> population;
  private final List<Future<?>> futures = new ArrayList<>();
  private final PauseController pauseController;
  private final ScoreBoard scoreBoard;
  private ExecutorService executorService;
  private final FightStrategy fightStrategy; // ✅ NUEVO

  private final int initialHealth;
  private final int damage;

  public ImmortalManager(int immortalsCount, int health, int damage, 
                        FightStrategy fightStrategy) { // ✅ NUEVO PARÁMETRO
    this.population = new ArrayList<>();
    this.scoreBoard = new ScoreBoard();
    this.pauseController = new PauseController();
    this.fightStrategy = fightStrategy; // ✅ NUEVO
    this.initialHealth = health;
    this.damage = damage;
    
    initializeImmortals(immortalsCount, health, damage);
    pauseController.setTotalThreads(population.size());
  }

  private void initializeImmortals(int count, int health, int damage) {
    for (int i = 0; i < count; i++) {
      String name = String.format("Immortal_%d", i);
      Immortal immortal = new Immortal(name, health, damage, population, 
                                     scoreBoard, pauseController, 
                                     fightStrategy); // ✅ PASAR STRATEGY
      population.add(immortal);
    }
  }

  public synchronized void start() {
    if (executorService != null) stop();
    executorService = Executors.newVirtualThreadPerTaskExecutor();
    for (Immortal im : population) {
      futures.add(executorService.submit(im));
    }
  }

  public void pause() throws InterruptedException { 
    pauseController.pause();
    pauseController.waitUntilAllPaused();
  }
  public void resume() { pauseController.resume(); }
  public void stop() {
    for (Immortal im : population) im.stopImmortal();
    if (executorService != null) executorService.shutdownNow();
  }

  public int aliveCount() {
    int c = 0;
    for (Immortal im : population) if (im.isImmortalAlive()) c++;
    return c;
  }

  public long totalHealth() {
    long sum = 0;
    for (Immortal im : population) sum += im.getHealth();
    return sum;
  }

  public List<Immortal> populationSnapshot() {
    return Collections.unmodifiableList(new ArrayList<>(population));
  }

  // ✅ GETTER PARA STRATEGY
  public FightStrategy getFightStrategy() {
    return fightStrategy;
  }

  // ✅ MÉTODO PARA VERIFICAR SI ESTÁ EJECUTÁNDOSE
  public boolean isRunning() {
    return executorService != null && !executorService.isShutdown();
  }

  public ScoreBoard scoreBoard() { return scoreBoard; }
  public PauseController controller() { return pauseController; }

  @Override public void close() { stop(); }
}

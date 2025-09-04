package edu.eci.arsw.immortals;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import edu.eci.arsw.concurrency.PauseController;

public class Immortal extends Thread {
  private final String name;
  private volatile int health;
  private final int damage;
  private final Collection<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController pauseController;
  private volatile boolean shouldStop = false;
  private final FightStrategy fightStrategy;

  /*
   * Constructor actualizado para incluir la estrategia de pelea.
   * - fightStrategy: Estrategia de pelea (NAIVE o ORDERED)
   */
  public Immortal(String name, int health, int damage, Collection<Immortal> population,
      ScoreBoard scoreBoard, PauseController pauseController,
      FightStrategy fightStrategy) {
    this.name = name;
    this.health = health;
    this.damage = damage;
    this.population = population;
    this.scoreBoard = scoreBoard;
    this.pauseController = pauseController;
    this.fightStrategy = fightStrategy;
  }

  public String name() {
    return name;
  }

  public synchronized int getHealth() {
    return health;
  }

  public boolean isImmortalAlive() {
    return getHealth() > 0 && !shouldStop;
  }

  public void stopImmortal() {
    shouldStop = true;
  }

  @Override
  public void run() {
    try {
      while (!shouldStop) {
        pauseController.awaitIfPaused();
        if (shouldStop) break;
        var opponent = pickOpponent();
        if (opponent == null)
          continue;
        fight(opponent);
        Thread.sleep(2);
        pauseController.awaitIfPaused();
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } finally {
      population.remove(this);
      pauseController.setTotalThreads(population.size());
    }
  }

  private Immortal pickOpponent() {
    Object[] arr = population.toArray(); 
    if (arr.length <= 1) return null;
    Immortal other;
    do {
      other = (Immortal) arr[ThreadLocalRandom.current().nextInt(arr.length)];
    } while (other == this);
    return other;
  }

  private void fight(Immortal opponent) {
    switch (this.fightStrategy) {
      case NAIVE -> fightNaive(opponent);
      case ORDERED -> fightOrdered(opponent);
      default -> fightOrdered(opponent);
    }
  }

  private void fightNaive(Immortal opponent) {
    synchronized (this) {
      synchronized (opponent) {
        doFight(opponent, "NAIVE");
      }
    }
  }

  private void fightOrdered(Immortal opponent) {
    Immortal first = this.name.compareTo(opponent.name) <= 0 ? this : opponent;
    Immortal second = this.name.compareTo(opponent.name) <= 0 ? opponent : this;

    synchronized (first) {
      synchronized (second) {
        doFight(opponent, "ORDERED");
      }
    }
  }
  private void doFight(Immortal opponent, String mode) {
    if (this.health > 0 && opponent.health > 0) {
      this.health += this.damage / 2;
      opponent.health = Math.max(0, opponent.health - this.damage);
      scoreBoard.recordFight();

      System.out.printf("[%s] %s attacks %s! (%d HP)%n",
          mode, this.name, opponent.name, opponent.health);

      if (opponent.health <= 0) {
        opponent.stopImmortal();
        boolean removed = population.remove(opponent);
        if (removed) {
          pauseController.setTotalThreads(population.size());
        }
      }
    }
  }
}

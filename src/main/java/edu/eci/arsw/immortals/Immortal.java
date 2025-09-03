package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Immortal extends Thread {
  private final String name;
  private volatile int health;
  private final int damage;
  private final List<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController pauseController;
  private volatile boolean shouldStop = false;
  private final FightStrategy fightStrategy;

  /*
   * Constructor actualizado para incluir la estrategia de pelea.
   * - fightStrategy: Estrategia de pelea (NAIVE o ORDERED)
   */
  public Immortal(String name, int health, int damage, List<Immortal> population,
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
        if (shouldStop)
          break;
        var opponent = pickOpponent();
        if (opponent == null)
          continue;
        fight(opponent);
        Thread.sleep(2);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  private Immortal pickOpponent() {
    if (population.size() <= 1)
      return null;
    Immortal other;
    do {
      other = population.get(ThreadLocalRandom.current().nextInt(population.size()));
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
        if (this.health > 0 && opponent.health > 0) {
          this.health += this.damage / 2;
          opponent.health -= this.damage;
          scoreBoard.recordFight();

          System.out.printf("⚔️ [NAIVE] %s attacks %s! (%d HP)%n",
              this.name, opponent.name, opponent.health);
        }
      }
    }
  }

  private void fightOrdered(Immortal opponent) {
    Immortal first = this.name.compareTo(opponent.name) <= 0 ? this : opponent;
    Immortal second = this.name.compareTo(opponent.name) <= 0 ? opponent : this;

    synchronized (first) {
      synchronized (second) {
        if (this.health > 0 && opponent.health > 0) {
          this.health += this.damage / 2;
          opponent.health = Math.max(0, opponent.health - this.damage);
          scoreBoard.recordFight();

          System.out.printf("[ORDERED] %s attacks %s! (%d HP)%n",
              this.name, opponent.name, opponent.health);
        }
      }
    }
  }
}

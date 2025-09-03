package edu.eci.arsw.app;

import edu.eci.arsw.demos.DeadlockDemo;
import edu.eci.arsw.demos.OrderedTransferDemo;
import edu.eci.arsw.demos.TryLockTransferDemo;
import edu.eci.arsw.immortals.FightStrategy;
import edu.eci.arsw.immortals.ImmortalManager;
import edu.eci.arsw.highlandersim.ControlFrame;

public class Main {
  public static void main(String[] args) throws Exception {
    String mode = System.getProperty("mode", "ui");
    
    if ("ui".equals(mode)) {
      runHighlanderSimulator();
    } else if ("demos".equals(mode)) {
      String demo = System.getProperty("demo", "2");
      switch (demo) {
        case "1" -> DeadlockDemo.run();
        case "2" -> OrderedTransferDemo.run();
        case "3" -> TryLockTransferDemo.run();
        default -> System.out.println("Use -Ddemo=1|2|3");
      }
    } else {
      System.out.println("Use -Dmode=ui|demos");
    }
  }

  private static void runHighlanderSimulator() {
    try {
      int count = Integer.parseInt(System.getProperty("count", "8"));
      int health = Integer.parseInt(System.getProperty("health", "100"));
      int damage = Integer.parseInt(System.getProperty("damage", "10"));
      FightStrategy strategy = parseFightStrategy(System.getProperty("fight", "ordered")); // ✅ NUEVO
      
      System.out.printf("🎮 Starting Highlander Simulator: %d immortals, %d HP, %d damage, %s strategy%n", 
          count, health, damage, strategy);
          
      ImmortalManager manager = new ImmortalManager(count, health, damage, strategy); // ✅ PASAR STRATEGY
      new ControlFrame(manager).setVisible(true);
      
    } catch (Exception e) {
      System.err.println("❌ Error starting simulator: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // ✅ NUEVO MÉTODO PARA PARSEAR STRATEGY
  private static FightStrategy parseFightStrategy(String strategy) {
    return switch (strategy.toLowerCase()) {
      case "naive" -> FightStrategy.NAIVE;
      case "ordered" -> FightStrategy.ORDERED;
      default -> {
        System.out.println("⚠️ Unknown strategy '" + strategy + "', using ORDERED");
        yield FightStrategy.ORDERED;
      }
    };
  }
}

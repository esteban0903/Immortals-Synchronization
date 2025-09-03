package edu.eci.arsw.highlandersim;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;
import edu.eci.arsw.immortals.FightStrategy;

public final class ControlFrame extends JFrame {

  private ImmortalManager manager;
  private final JTextArea output = new JTextArea(14, 40);
  private final JLabel statusLabel = new JLabel("Status: Stopped"); // ✅ NUEVO LABEL PARA STATUS
  private final JButton startBtn = new JButton("Start");
  private final JButton pauseAndCheckBtn = new JButton("Pause & Check");
  private final JButton resumeBtn = new JButton("Resume");
  private final JButton stopBtn = new JButton("Stop");

  private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 5000, 1));
  private final JSpinner healthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
  private final JSpinner damageSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
  private final JComboBox<String> fightMode = new JComboBox<>(new String[]{"ordered", "naive"});

  public ControlFrame(int count, String fight) {
    setTitle("Highlander Simulator — ARSW");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8,8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Count:"));
    countSpinner.setValue(count);
    top.add(countSpinner);
    top.add(new JLabel("Health:"));
    top.add(healthSpinner);
    top.add(new JLabel("Damage:"));
    top.add(damageSpinner);
    top.add(new JLabel("Fight:"));
    fightMode.setSelectedItem(fight);
    top.add(fightMode);
    add(top, BorderLayout.NORTH);

    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(statusLabel, BorderLayout.NORTH); // ✅ AÑADIR STATUS LABEL
    centerPanel.add(new JScrollPane(output), BorderLayout.CENTER);
    add(centerPanel, BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.add(startBtn);
    bottom.add(pauseAndCheckBtn);
    bottom.add(resumeBtn);
    bottom.add(stopBtn);
    add(bottom, BorderLayout.SOUTH);

    startBtn.addActionListener(this::onStart);
    pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
    resumeBtn.addActionListener(this::onResume);
    stopBtn.addActionListener(this::onStop);

    pack();
    setLocationByPlatform(true);
  }

  // ✅ NUEVO CONSTRUCTOR QUE ACEPTA IMMORTALMANAGER
  public ControlFrame(ImmortalManager manager) {
    this.manager = manager;
    setTitle("Highlander Simulator — ARSW");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8,8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Count:"));
    top.add(countSpinner);
    top.add(new JLabel("Health:"));
    top.add(healthSpinner);
    top.add(new JLabel("Damage:"));
    top.add(damageSpinner);
    top.add(new JLabel("Fight:"));
    top.add(fightMode);
    add(top, BorderLayout.NORTH);

    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(statusLabel, BorderLayout.NORTH); // ✅ AÑADIR STATUS LABEL
    centerPanel.add(new JScrollPane(output), BorderLayout.CENTER);
    add(centerPanel, BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.add(startBtn);
    bottom.add(pauseAndCheckBtn);
    bottom.add(resumeBtn);
    bottom.add(stopBtn);
    add(bottom, BorderLayout.SOUTH);

    startBtn.addActionListener(this::onStart);
    pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
    resumeBtn.addActionListener(this::onResume);
    stopBtn.addActionListener(this::onStop);

    pack();
    setLocationByPlatform(true);
  }

  // ✅ NUEVO MÉTODO PARA ACTUALIZAR DISPLAY
  private void updateDisplay() {
    if (manager == null) return;
    
    try {
      StringBuilder status = new StringBuilder();
      status.append("Status: ").append(manager.isRunning() ? "Running" : "Stopped");
      status.append(" | Strategy: ").append(manager.getFightStrategy()); // ✅ MOSTRAR STRATEGY
      status.append(" | Battles: ").append(manager.scoreBoard().totalFights());
      
      statusLabel.setText(status.toString());
      
      // Update display with current immortals info
      List<Immortal> pop = manager.populationSnapshot();
      StringBuilder sb = new StringBuilder();
      long sum = 0;
      for (Immortal im : pop) {
        int h = im.getHealth();
        sum += h;
        sb.append(String.format("%-14s : %5d%n", im.name(), h));
      }
      sb.append("--------------------------------\n");
      sb.append("Total Health: ").append(sum).append('\n');
      sb.append("Alive Count: ").append(manager.aliveCount()).append('\n');
      
      output.setText(sb.toString());
      
    } catch (Exception e) {
      statusLabel.setText("Error updating display: " + e.getMessage());
    }
  }

  private void onStart(ActionEvent e) {
    safeStop();
    int n = (Integer) countSpinner.getValue();
    int health = (Integer) healthSpinner.getValue();
    int damage = (Integer) damageSpinner.getValue();
    String fight = (String) fightMode.getSelectedItem();
    FightStrategy strategy = parseFightStrategy(fight); // ✅ PARSEAR STRATEGY
    manager = new ImmortalManager(n, health, damage, strategy); // ✅ USAR NUEVO CONSTRUCTOR
    manager.start();
    output.setText("Simulation started with %d immortals (health=%d, damage=%d, fight=%s)%n"
      .formatted(n, health, damage, strategy));
    updateDisplay(); // ✅ ACTUALIZAR DISPLAY
  }

  // ✅ MÉTODO PARA PARSEAR STRATEGY
  private static FightStrategy parseFightStrategy(String strategy) {
    return switch (strategy.toLowerCase()) {
      case "naive" -> FightStrategy.NAIVE;
      case "ordered" -> FightStrategy.ORDERED;
      default -> FightStrategy.ORDERED;
    };
  }

  private void onPauseAndCheck(ActionEvent e) {
      if (manager == null) return;
      new Thread(() -> {
          try {
              manager.pause(); // Bloqueante pero ya no congela la GUI
              SwingUtilities.invokeLater(this::updateDisplay); // ✅ USAR updateDisplay()
          } catch (InterruptedException ex) {
              ex.printStackTrace();
          }
      }).start();
  }

  private void onResume(ActionEvent e) {
    if (manager == null) return;
    manager.resume();
    updateDisplay(); // ✅ ACTUALIZAR DISPLAY
  }

  private void onStop(ActionEvent e) { 
    safeStop(); 
    updateDisplay(); // ✅ ACTUALIZAR DISPLAY
  }

  private void safeStop() {
    if (manager != null) {
      manager.stop();
      manager = null;
    }
  }

  public static void main(String[] args) {
    int count = Integer.getInteger("count", 8);
    String fight = System.getProperty("fight", "ordered");
    SwingUtilities.invokeLater(() -> new ControlFrame(count, fight));
  }
}

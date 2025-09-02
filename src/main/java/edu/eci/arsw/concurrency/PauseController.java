package edu.eci.arsw.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  private volatile boolean paused = false;
  private int pausedThreads = 0;
  private int totalThreads = 0;
  private final Condition allPaused = lock.newCondition();

  public void pause() { lock.lock(); try { paused = true; } finally { lock.unlock(); } }
  public void resume() { lock.lock(); try { paused = false; unpaused.signalAll(); } finally { lock.unlock(); } }
  public boolean paused() { return paused; }

  public void awaitIfPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try { 
      while (paused) {
        pausedThreads++;
        if (pausedThreads == totalThreads) {
          allPaused.signalAll();
        }
        try {
          unpaused.await();
        } finally {
          pausedThreads--;
        }
      }

    } finally { 
      lock.unlock(); 
    }
  }

  public void waitUntilAllPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (pausedThreads < totalThreads) {
        allPaused.await();
      }
    } finally {
      lock.unlock();
    }
  }
  public void setTotalThreads(int n) {
    lock.lock();
    try {
        totalThreads = n;
    } finally {
        lock.unlock();
    }
  }
}

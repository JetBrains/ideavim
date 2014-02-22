package com.maddyhome.idea.vim.insert;

public class InsertToCommandStateTimer {
  public static final long DEFAULT_KEY_SEQUENCE_TIMEOUT = 1000;
  private final long keySequenceTimeoutMillis;
  private boolean timeoutInProgress;
  private long beginTimeMillis;

  public InsertToCommandStateTimer() {
    this(DEFAULT_KEY_SEQUENCE_TIMEOUT);
  }

  public InsertToCommandStateTimer(long keySequenceTimeoutMillis) {
    this.keySequenceTimeoutMillis = keySequenceTimeoutMillis;
    this.timeoutInProgress = false;
  }

  public void resetAndBeginTimer() {
    timeoutInProgress = true;
    beginTimeMillis = System.currentTimeMillis();
  }

  public boolean timeoutElapsed() {
    long difference = System.currentTimeMillis() - beginTimeMillis;
    return timeoutInProgress && (difference >= keySequenceTimeoutMillis);
  }

  public void stopTimer() {
    timeoutInProgress = false;
  }
}

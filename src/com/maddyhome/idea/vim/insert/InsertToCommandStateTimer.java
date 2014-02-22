/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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

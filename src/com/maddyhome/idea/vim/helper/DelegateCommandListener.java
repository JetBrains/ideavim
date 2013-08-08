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

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.command.CommandAdapter;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.maddyhome.idea.vim.common.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DelegateCommandListener extends CommandAdapter {
  @NotNull
  public static DelegateCommandListener getInstance() {
    return instance;
  }

  public void setRunnable(@Nullable StartFinishRunnable runnable) {
    this.runnable = runnable;
    inCommand = false;
  }

  public void commandStarted(@NotNull CommandEvent event) {
    inCommand = true;
    if (logger.isDebugEnabled()) {
      logger.debug("Command started: " + event);
      logger.debug("Name: " + event.getCommandName());
      logger.debug("Group: " + event.getCommandGroupId());
    }

    if (runnable != null) {
      runnable.start();
    }
  }

  public void commandFinished(@NotNull CommandEvent event) {
    if (logger.isDebugEnabled()) {
      logger.debug("Command finished: " + event);
      logger.debug("Name: " + event.getCommandName());
      logger.debug("Group: " + event.getCommandGroupId());
    }

    if (runnable != null && inCommand) {
      runnable.finish();
      runnable = null;
    }

    inCommand = false;
  }

  private DelegateCommandListener() {
  }

  public static interface StartFinishRunnable {
    @Nullable
    TextRange start();

    void finish();
  }

  private boolean inCommand = false;
  @Nullable private StartFinishRunnable runnable;

  private static Logger logger = Logger.getInstance(DelegateCommandListener.class.getName());
  @NotNull private static DelegateCommandListener instance = new DelegateCommandListener();
}
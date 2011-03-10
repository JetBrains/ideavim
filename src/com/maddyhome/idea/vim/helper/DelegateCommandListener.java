package com.maddyhome.idea.vim.helper;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.command.CommandAdapter;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.maddyhome.idea.vim.common.TextRange;

public class DelegateCommandListener extends CommandAdapter {
  public static DelegateCommandListener getInstance() {
    return instance;
  }

  public void setRunnable(StartFinishRunnable runnable) {
    this.runnable = runnable;
    inCommand = false;
  }

  public StartFinishRunnable getRunnable() {
    return runnable;
  }

  public void commandStarted(CommandEvent event) {
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

  public void commandFinished(CommandEvent event) {
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
    TextRange start();

    void finish();
  }

  private boolean inCommand = false;
  private StartFinishRunnable runnable;

  private static Logger logger = Logger.getInstance(DelegateCommandListener.class.getName());
  private static DelegateCommandListener instance = new DelegateCommandListener();
}
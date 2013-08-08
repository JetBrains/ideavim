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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This provides some helper methods to run code as a command and an application write action
 */
public class RunnableHelper {
  private static final Logger logger = Logger.getInstance(KeyHandler.class.getName());

  private RunnableHelper() {}

  public static void runReadCommand(@Nullable Project project, @NotNull Runnable cmd, @Nullable String name, @Nullable Object groupId) {
    if (logger.isDebugEnabled()) {
      logger.debug("read command " + cmd);
    }
    CommandProcessor.getInstance().executeCommand(project, new ReadAction(cmd), name, groupId);
  }

  public static void runWriteCommand(@Nullable Project project, @NotNull Runnable cmd, @Nullable String name, @Nullable Object groupId) {
    if (logger.isDebugEnabled()) {
      logger.debug("write command " + cmd);
    }
    CommandProcessor.getInstance().executeCommand(project, new WriteAction(cmd), name, groupId);
  }

  static class ReadAction implements Runnable {
    @NotNull private final Runnable cmd;

    ReadAction(@NotNull Runnable cmd) {
      this.cmd = cmd;
    }

    public void run() {
      ApplicationManager.getApplication().runReadAction(cmd);
    }
  }

  static class WriteAction implements Runnable {
    @NotNull private final Runnable cmd;

    WriteAction(@NotNull Runnable cmd) {
      this.cmd = cmd;
    }

    public void run() {
      ApplicationManager.getApplication().runWriteAction(cmd);
    }
  }
}

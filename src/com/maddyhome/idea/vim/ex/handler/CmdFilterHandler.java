/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2014 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class CmdFilterHandler extends CommandHandler {
  public CmdFilterHandler() {
    super("!", "", RANGE_OPTIONAL | ARGUMENT_OPTIONAL | WRITABLE);
  }

  public static final Pattern filePlaceholder = Pattern.compile("([\\\\]?)%");

  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd)
    throws ExException {
    logger.info("execute");

    String command = cmd.getArgument();
    if (command.length() == 0) {
      return false;
    }

    if (command.indexOf('!') != -1) {
      String last = VimPlugin.getProcess().getLastCommand();
      if (last == null || last.length() == 0) {
        VimPlugin.showMessage(MessageHelper.message(Msg.e_noprev));
        return false;
      }
      command = command.replaceAll("!", last);
    }

    final VirtualFile virtualFile = EditorData.getVirtualFile(editor);
    if (command.contains("%") && virtualFile != null && virtualFile.isInLocalFileSystem()) {
      command = replaceFilePlaceholder(command, virtualFile);
    }

    try {
      Ranges ranges = cmd.getRanges();
      if (ranges.size() == 0) {
        // Show command output in a window
        String commandOutput = VimPlugin.getProcess().executeCommand(command, null);
        ExOutputModel.getInstance(editor).output(commandOutput);
        return true;
      }
      else {
        // Filter
        TextRange range = cmd.getTextRange(editor, context, false);
        return VimPlugin.getProcess().executeFilter(editor, range, command);
      }
    }
    catch (IOException e) {
      throw new ExException(e.getMessage());
    }
  }

  private String replaceFilePlaceholder(String command, VirtualFile virtualFile) {
    StringBuffer commandBuffer = new StringBuffer(command.length());
    Matcher matcher = filePlaceholder.matcher(command);
    while (matcher.find()) {
      if (matcher.group(1).length() > 0) {
        matcher.appendReplacement(commandBuffer, "%");
      }
      else {
        matcher.appendReplacement(commandBuffer, virtualFile.getPath());
      }
    }
    matcher.appendTail(commandBuffer);
    return commandBuffer.toString();
  }

  private static final Logger logger = Logger.getInstance(CmdFilterHandler.class.getName());
}

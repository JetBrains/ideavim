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

package com.maddyhome.idea.vim.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.annotations.NotNull;

/**
 */
public abstract class AbstractEditorActionHandler extends EditorActionHandler {
  public final void execute(@NotNull Editor editor, @NotNull DataContext context) {
    editor = InjectedLanguageUtil.getTopLevelEditor(editor);
    logger.debug("execute");
    if (!VimPlugin.isEnabled() && this instanceof DelegateActionHandler) {
      KeyHandler.executeAction(((DelegateActionHandler)this).getOrigAction(), context);

      return;
    }

    final CommandState state = CommandState.getInstance(editor);
    final Command cmd = state.getCommand();

    if (cmd == null || !execute(editor, context, cmd)) {
      VimPlugin.indicateError();
    }
  }

  public void process(Command cmd) {
    // No-op
  }

  protected abstract boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd);

  private static Logger logger = Logger.getInstance(AbstractEditorActionHandler.class.getName());
}

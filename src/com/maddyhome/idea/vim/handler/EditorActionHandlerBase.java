/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public abstract class EditorActionHandlerBase extends EditorActionHandler {
  protected boolean myRunForEachCaret;

  public EditorActionHandlerBase() {
    this(false);
  }

  public EditorActionHandlerBase(boolean runForEachCaret) {
    super(runForEachCaret);
    myRunForEachCaret = runForEachCaret;
  }

  @Override
  public final void execute(@NotNull Editor editor, @NotNull DataContext context) {
    editor = InjectedLanguageUtil.getTopLevelEditor(editor);
    doExecute(editor, editor.getCaretModel().getPrimaryCaret(), context);
 }

  @Override
  public final void doExecute(@NotNull Editor editor, @Nullable Caret caret, @NotNull DataContext context) {
    editor = InjectedLanguageUtil.getTopLevelEditor(editor);
    logger.debug("doExecute");

    if (!VimPlugin.isEnabled()) {
      return;
    }

    final CommandState state = CommandState.getInstance(editor);
    final Command cmd = state.getCommand();

    if (cmd == null || !execute(editor, caret, context, cmd)) {
      VimPlugin.indicateError();
    }
  }

  public void process(Command cmd) {
    // No-op
  }

  /**
   * @deprecated To implement the action logic, override {@link #execute(Editor, Caret, DataContext, Command)}.
   */
  protected boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull Command cmd) {
    return execute(editor, editor.getCaretModel().getPrimaryCaret(), context, cmd);
  }

  protected boolean execute(@NotNull Editor editor, @Nullable Caret caret, @NotNull DataContext context,
                            @NotNull Command cmd) {
    return execute(editor, context, cmd);
  }

  private static final Logger logger = Logger.getInstance(EditorActionHandlerBase.class.getName());
}

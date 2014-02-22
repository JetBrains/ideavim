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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.key.BranchNode;
import com.maddyhome.idea.vim.key.CommandNode;
import com.maddyhome.idea.vim.key.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class InsertToCommandStateHandler {
  private final InsertToCommandStateTimer timer;
  private final InsertToCommandState insertToCommandState;
  private List<KeyStroke> bufferedKeys;

  public InsertToCommandStateHandler() {
    this.bufferedKeys = new ArrayList<KeyStroke>(10);
    this.timer = new InsertToCommandStateTimer();
    this.insertToCommandState = new InsertToCommandState(timer, bufferedKeys);
  }

  public void outputBufferedKeysFromFailedInsertToCommandStateChange(@NotNull final Editor editor,
                                                                     @NotNull final CommandState editorState,
                                                                     @NotNull final DataContext dataContext,
                                                                     @Nullable final Node node)
    throws TimeoutElaspedException {

    final boolean[] timedOut = {false};
    insertToCommandState.accept(editorState, node, new InsertToCommandStateVisitor() {
      @Override
      public void NotInInsertToCommandState() {
      }

      @Override
      public void TimedOutValidCommandSequence() {
        for (KeyStroke key : bufferedKeys) {
          CommandGroups.getInstance().getChange().processKey(editor, dataContext, key);
        }
        bufferedKeys.clear();
        timer.stopTimer();
        timedOut[0] = true;
      }

      @Override
      public void EndingInsertToCommandSequenceByChangingToCommandMode(@NotNull CommandNode node) {
        timer.stopTimer();
        bufferedKeys.clear();
      }

      @Override
      public void BeginningInsertToCommandSequence(@NotNull BranchNode node) {
        timer.resetAndBeginTimer();
        bufferedKeys.add(node.getKey());
      }

      @Override
      public void ContinuingInsertToCommandSequence(@NotNull BranchNode node) {
        bufferedKeys.add(node.getKey());
      }

      @Override
      public void EndingInsertToCommandSequenceByStayingInInsertMode() {
        timer.stopTimer();
        for (KeyStroke key : bufferedKeys) {
          CommandGroups.getInstance().getChange().processKey(editor, dataContext, key);
        }
        bufferedKeys.clear();
      }
    });

    if (timedOut[0]) {
      throw new TimeoutElaspedException();
    }
  }
}

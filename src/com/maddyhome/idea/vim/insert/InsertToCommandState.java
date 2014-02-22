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

import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.key.BranchNode;
import com.maddyhome.idea.vim.key.CommandNode;
import com.maddyhome.idea.vim.key.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.KeyStroke;
import java.util.List;

public class InsertToCommandState {

  private final List<KeyStroke> bufferedKeys;
  private final InsertToCommandStateTimer timer;

  public InsertToCommandState(InsertToCommandStateTimer timer, List<KeyStroke> bufferedKeys) {
    this.bufferedKeys = bufferedKeys;
    this.timer = timer;
  }

  public void accept(@NotNull CommandState editorState, @Nullable Node node, @NotNull InsertToCommandStateVisitor visitor) {
    if (editorState.getMode() != CommandState.Mode.INSERT) {
      visitor.NotInInsertToCommandState();
    }
    else if (timer.timeoutElapsed()) {
      visitor.TimedOutValidCommandSequence();
    }
    else if (node instanceof CommandNode) {
      visitor.EndingInsertToCommandSequenceByChangingToCommandMode((CommandNode)node);
    }
    else if (node instanceof BranchNode && bufferedKeys.isEmpty()) {
      visitor.BeginningInsertToCommandSequence((BranchNode)node);
    }
    else if (node instanceof BranchNode && !bufferedKeys.isEmpty()) {
      visitor.ContinuingInsertToCommandSequence((BranchNode)node);
    }
    else if (node == null && !bufferedKeys.isEmpty()) {
      visitor.EndingInsertToCommandSequenceByStayingInInsertMode();
    }
    else {
      visitor.NotInInsertToCommandState();
    }
  }
}

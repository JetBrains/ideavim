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

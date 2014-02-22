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

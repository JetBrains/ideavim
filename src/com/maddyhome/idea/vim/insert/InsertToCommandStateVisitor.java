package com.maddyhome.idea.vim.insert;

import com.maddyhome.idea.vim.key.BranchNode;
import com.maddyhome.idea.vim.key.CommandNode;
import org.jetbrains.annotations.NotNull;

public interface InsertToCommandStateVisitor {
  public void NotInInsertToCommandState();
  public void TimedOutValidCommandSequence();
  public void EndingInsertToCommandSequenceByChangingToCommandMode(@NotNull CommandNode node);
  public void BeginningInsertToCommandSequence(@NotNull BranchNode node);
  public void ContinuingInsertToCommandSequence(@NotNull BranchNode node);
  public void EndingInsertToCommandSequenceByStayingInInsertMode();



}

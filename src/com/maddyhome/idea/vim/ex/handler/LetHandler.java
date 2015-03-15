package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.VimScriptCommandHandler;
import com.maddyhome.idea.vim.var.Variables;
import org.jetbrains.annotations.NotNull;

/** The let command handler for vimscript.
 * Created by psjay on 15/3/14.
 */
public class LetHandler extends CommandHandler implements VimScriptCommandHandler {
  public LetHandler() {
    super("let", "", ARGUMENT_OPTIONAL);
  }
  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd)
    throws ExException {
    return Variables.parseVariableLine(cmd.getArgument());
  }

  @Override
  public void execute(@NotNull ExCommand cmd) throws ExException {
    Variables.parseVariableLine(cmd.getArgument());
  }
}

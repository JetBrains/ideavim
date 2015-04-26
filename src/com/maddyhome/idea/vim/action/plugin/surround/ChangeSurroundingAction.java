package com.maddyhome.idea.vim.action.plugin.surround;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.GetCharListener;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author dhleong
 */
public class ChangeSurroundingAction extends EditorAction {
  protected ChangeSurroundingAction() {
    super(new Handler());
  }

  private static class Handler extends EditorActionHandlerBase {

    @Override
    protected final boolean execute(@NotNull final Editor editor, @NotNull DataContext context, @NotNull final Command cmd) {
      final Argument argument = cmd.getArgument();
      if (argument == null) {
        return false;
      }

      if (argument.getType() == Argument.Type.STRING
          && StringUtil.length(argument.getString()) == 2) {
        // repeat previous command
        final String parts = argument.getString();
        assert parts != null;
        performChange(editor, parts.charAt(0), parts.charAt(1));
        CommandState.getInstance(editor).saveLastChangeCommand(cmd);
      } else {

        KeyHandler.getInstance().getChar(new GetCharListener() {
          @Override
          public void onCharTyped(KeyStroke key, char chKey) {
            if (KeyHandler.isCancelStroke(key)) {
              return;
            }

            performChange(editor, argument.getCharacter(), chKey);

            // make this repeatable by replacing the char arg with a 2-char str
            // NB: If performChange requests more chars (as in `t`) then
            //  this will not be sufficient to repeat the command
            cmd.setArgument(new Argument("" + argument.getCharacter() + chKey));
            CommandState.getInstance(editor).saveLastChangeCommand(cmd);
          }

        });
      }

      return true;
    }

    private void performChange(
        final Editor editor, final char charFrom, final char charTo) {
      PairExtractor.extract(charTo, new PairExtractor.PairListener() {
        @Override
        public void onPair(SurroundPair pair) {
          // now, perform the change!
          SurroundingChanger.change(editor, charFrom, pair);
        }
      });
    }
  }
}

package com.maddyhome.idea.vim.action.plugin.surround;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.GetCharListener;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author dhleong
 */
public class ChangeSurroundingAction extends EditorAction {
  protected ChangeSurroundingAction() {
    super(new Handler());
  }

  private static class Handler extends ChangeEditorActionHandler {

    @Override
    public boolean execute(@NotNull final Editor editor,
                           @NotNull final DataContext context,
                           final int count,
                           final int rawCount,
                           @Nullable final Argument argument) {

      if (argument == null) {
        return false;
      }

      if (CommandState.inRepeatMode(editor)
          && argument instanceof ChangeSurroundMotionArgument) {

        // repeat previous command
        final char charFrom = argument.getCharacter();
        final SurroundPair pair =
          ((ChangeSurroundMotionArgument)argument).surroundPair;
        SurroundingChanger.change(editor, charFrom, pair);
      } else {

        KeyHandler.getInstance().getChar(new GetCharListener() {
          @Override
          public void onCharTyped(KeyStroke key, char chKey) {
            if (KeyHandler.isCancelStroke(key)) {
              return;
            }

            extractPairAndChange(editor, argument.getCharacter(), chKey);
          }

        });
      }

      return true;
    }

    static void extractPairAndChange(
      final Editor editor, final char charFrom, final char charTo) {
      PairExtractor.extract(charTo, new PairExtractor.PairListener() {
        @Override
        public void onPair(SurroundPair pair) {
          // now, perform the change!
          SurroundingChanger.change(editor, charFrom, pair);

          // make this repeatable by subclassing Argument with our extra args
          final Command current = CommandState.getInstance(editor).getCommand();
          if (current != null) {
            // it shouldn't be null...
            current.setArgument(new ChangeSurroundMotionArgument(
              charFrom, pair));
          }
        }
      });
    }
  }

  static class ChangeSurroundMotionArgument extends Argument {

    final SurroundPair surroundPair;

    public ChangeSurroundMotionArgument(char charFrom, SurroundPair surroundPair) {
      super(charFrom);

      this.surroundPair = surroundPair;
    }
  }
}

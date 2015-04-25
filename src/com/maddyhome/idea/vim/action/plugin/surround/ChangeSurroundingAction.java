package com.maddyhome.idea.vim.action.plugin.surround;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.GetCharListener;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;

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
                           @NotNull DataContext context,
                           int count,
                           int rawCount,
                           @Nullable final Argument argument) {

      if (argument == null) {
        return false;
      }

      // TODO make this repeatable by saving the char somewhere?
      KeyHandler.getInstance().getChar(new GetCharListener() {
        @Override
        public void onCharTyped(KeyStroke key, char chKey) {
          if (key.getKeyCode() == KeyEvent.VK_ESCAPE
              || ((key.getModifiers() | KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK
                  && key.getKeyCode() == KeyEvent.VK_C)) {
            // action canceled
            return;
          }

          PairExtractor.extract(chKey, new PairExtractor.PairListener() {
            @Override
            public void onPair(SurroundPair pair) {

              KeyHandler.getInstance().reset(editor);

              // now, perform the change!
              final char chKey = argument.getCharacter();

              SurroundingChanger.change(editor, chKey, pair);
            }
          });
        }
      });

      return true;
    }
  }
}

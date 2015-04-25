package com.maddyhome.idea.vim.action.plugin.surround;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.maddyhome.idea.vim.GetCharListener;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author dhleong
 */
public class SurroundMotionAction extends EditorAction {
  public SurroundMotionAction() {
    super(new Handler());
  }

  private static class Handler extends ChangeEditorActionHandler {

    @Override
    public boolean execute(@NotNull final Editor editor,
                           @NotNull final DataContext context,
                           final int count,
                           final int rawCount,
                           @Nullable final Argument argument) {

      final Command motion = argument.getMotion();
      if (motion == null) {
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

          final VimSurrounder surrounder =
            new VimSurrounder(editor, context, count, rawCount, argument);
          PairExtractor.extract(chKey, surrounder);
        }
      });

      return true;
    }
  }


  static class VimSurrounder implements PairExtractor.PairListener {

    private final Editor myEditor;
    private final DataContext myContext;
    private final int myCount;
    private final int myRawCount;
    private final Argument myArgument;

    public VimSurrounder(Editor editor, DataContext context, int count, int rawCount, Argument argument) {
      myEditor = editor;
      myContext = context;
      myCount = count;
      myRawCount = rawCount;
      myArgument = argument;
    }

    @Override
    public void onPair(final SurroundPair pair) {
      final TextRange range = MotionGroup.getMotionRange(myEditor, myContext, myCount, myRawCount, myArgument, true);
      final int before = range.getStartOffset();
      final int after = range.getEndOffset();

      final Runnable action = new Runnable() {
        @Override
        public void run() {
          final ChangeGroup change = VimPlugin.getChange();
          change.insertText(myEditor, after, pair.after);
          change.insertText(myEditor, before, pair.before);
        }
      };

      RunnableHelper.runWriteCommand(
        myEditor.getProject(), action, SurroundPlugin.NAME, action);
    }
  }
}

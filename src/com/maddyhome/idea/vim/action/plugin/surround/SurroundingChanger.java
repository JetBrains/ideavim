package com.maddyhome.idea.vim.action.plugin.surround;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Mark;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * The SurroundingChanger works in three steps,
 *    based on the original vim-surround:
 *
 *  1. Extract inner
 *    Copy the contents "inside" the surrounding
 *    indicated by the given key. This may be performed
 *    by deleting them, but is not necessary
 *
 *  2. Remove outer
 *    Delete the contents "outside" the surrounding
 *    (in the vim sense, that is the "inside" plus
 *    the surrounding).
 *
 *  3. Paste Surrounded
 *    If desired, surround the copied "inside" contents
 *    in some way, then paste it into place
 *
 * @author dhleong
 */
public abstract class SurroundingChanger {

  static final SurroundingChanger[] CHANGERS = {
    // Register Changer instances here
    new PunctualChanger()
  };

  /** Handles punctuation and xml tags */
  static class PunctualChanger extends SurroundingChanger {

    static final String ACCEPTS = "b()B{}r[]a<>`'\"t";

    @Override
    boolean handles(char chKey) {
      return -1 != ACCEPTS.indexOf(chKey);
    }

    @Override
    public List<KeyStroke> extractInner(Editor editor, char chKey) {
      perform(editor, "di" + pick(chKey));
      return getContentsOf(REGISTER);
    }

    @Override
    public void removeOuter(Editor editor, char chKey) {
      perform(editor, "da" + pick(chKey));
    }

    @Override
    public void pasteSurrounded(Editor editor) {
      // this logic is direct from vim-surround
      final int offset = editor.getCaretModel().getOffset();
      final int line = editor.getDocument().getLineNumber(offset);
      final int lineEnd = editor.getDocument().getLineEndOffset(line);

      final Mark mark = VimPlugin.getMark().getMark(editor, ']');
      final int motionEndCol = mark == null ? -1 : mark.getCol();
      final String pasteCommand;
      if (motionEndCol == lineEnd && offset + 1 == lineEnd) {
        pasteCommand = "p";
      } else {
        pasteCommand = "P";
      }

      perform(editor, pasteCommand);
    }

    static char pick(char chKey) {
      switch (chKey) {
        case 'a': return '>';
        case 'r': return ']';
        default: return chKey;
      }
    }

  }

  /** @return True if this Changer can handle the given char */
  abstract boolean handles(char chKey);
  /** Perform step 1 */
  public abstract List<KeyStroke> extractInner(Editor editor, char chKey);
  /** Perform step 2 */
  public abstract void removeOuter(Editor editor, char chKey);
  /** Perform step 3 */
  public abstract void pasteSurrounded(Editor editor);

  private static void performChange(
      final Editor editor, final char chKey, @Nullable final SurroundPair surround,
      final SurroundingChanger changer) {

    // save the current command (we don't want to override it)
    final CommandState state = CommandState.getInstance(editor);
    final Command currentCommand = state.getCommand();
    final Command lastChangeCommand = state.getLastChangeCommand();

    // reset the KeyHandler state so we can perform our actions
    KeyHandler.getInstance().reset(editor);

    RunnableHelper.runWriteCommand(editor.getProject(), new Runnable() {
      @Override
      public void run() {
        // we take over the " register, so preserve it
        final List<KeyStroke> oldValue = getContentsOf(REGISTER);

        // extract inner
        final List<KeyStroke> innerValue = changer.extractInner(editor, chKey);

        // delete the surrounding
        changer.removeOuter(editor, chKey);

        // paste the innerValue
        if (surround != null) {
          innerValue.addAll(0, parseKeys(surround.before));
          innerValue.addAll(parseKeys(surround.after));
        }
        setContentsOf(REGISTER, innerValue);
        changer.pasteSurrounded(editor);

        // restore the old value
        setContentsOf(REGISTER, oldValue);
      }
    }, null, null);

    // restore
    if (currentCommand != null) {
      state.setCommand(currentCommand);
    }
    state.saveLastChangeCommand(lastChangeCommand);
  }

  static List<KeyStroke> getContentsOf(char register) {
    final Register reg = VimPlugin.getRegister().getRegister(register);
    if (reg == null) {
      return null;
    }
    return reg.getKeys();
  }

  static void setContentsOf(char register, List<KeyStroke> keys) {
    if (keys == null) {
      return;
    }

    VimPlugin.getRegister().setKeys(register, keys);
  }

  static void perform(final Editor editor, final String sequence) {

    // store everything into our temp register
    final List<KeyStroke> keys = parseKeys(
      "\"" + REGISTER + sequence
    );

    final EditorDataContext dataContext =
      new EditorDataContext(editor);
    final KeyHandler keyHandler = KeyHandler.getInstance();
    for (KeyStroke key : keys) {
      keyHandler.handleKey(editor, key, dataContext, false);
    }
  }

  /**
   * Attempt to change the surroundings at the cursor
   *
   * @param chKey The type of surroundings to change
   * @param pair The new surroundings, if any
   * @return True if we were able to change, else false
   */
  public static boolean change(Editor editor, char chKey, @Nullable SurroundPair pair) {
    for (SurroundingChanger changer : CHANGERS) {
      if (changer.handles(chKey)) {
        performChange(editor, chKey, pair, changer);
        return true;
      }
    }
    return false;
  }

  static final char REGISTER = '"';
}

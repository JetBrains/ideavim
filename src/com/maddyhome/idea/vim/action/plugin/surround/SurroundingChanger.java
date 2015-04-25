package com.maddyhome.idea.vim.action.plugin.surround;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
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

  static SurroundingChanger[] CHANGERS = {
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
      perform(editor, "\"" + REGISTER + "di" + chKey);
      return getContentsOf(REGISTER);
    }

    @Override
    public void removeOuter(Editor editor, char chKey) {
      perform(editor, "\"" + REGISTER + "da" + chKey);
    }

    @Override
    public void pasteSurrounded(Editor editor) {
      perform(editor, "\"" + REGISTER + "P");
    }
  }

  abstract boolean handles(char chKey);
  public abstract List<KeyStroke> extractInner(Editor editor, char chKey);
  public abstract void removeOuter(Editor editor, char chKey);
  public abstract void pasteSurrounded(Editor editor);

  private static void performChange(
      final Editor editor, final char chKey, @Nullable final String surround,
      final SurroundingChanger changer) {

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
          List<KeyStroke> surrounding = parseKeys(surround);
          innerValue.addAll(0, surrounding);
          innerValue.addAll(surrounding);
        }
        setContentsOf(REGISTER, innerValue);
        changer.pasteSurrounded(editor);

        // restore the old value
        setContentsOf(REGISTER, oldValue);
      }
    }, null, null);
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
    final EditorDataContext dataContext =
      new EditorDataContext(editor);
    final KeyHandler keyHandler = KeyHandler.getInstance();
    for (KeyStroke key : parseKeys(sequence)) {
      keyHandler.handleKey(editor, key, dataContext, false);
    }
  }

  /**
   * Attempt to change the surroundings at the cursor
   *
   * @param chKey The type of surroundings to change
   * @param surround The new surroundings, if any
   * @return True if we were able to change, else false
   */
  public static boolean change(Editor editor, char chKey, @Nullable String surround) {
    for (SurroundingChanger changer : CHANGERS) {
      if (changer.handles(chKey)) {
        performChange(editor, chKey, surround, changer);
        return true;
      }
    }
    return false;
  }

  static final char REGISTER = '"';
}

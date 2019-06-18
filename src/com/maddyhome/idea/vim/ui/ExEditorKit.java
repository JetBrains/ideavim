/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.helper.DigraphSequence;
import com.maddyhome.idea.vim.helper.SearchHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 */
public class ExEditorKit extends DefaultEditorKit {
  public static ExEditorKit getInstance() {
    if (instance == null) {
      instance = new ExEditorKit();
    }

    return instance;
  }

  /**
   * Gets the MIME type of the data that this
   * kit represents support for.
   *
   * @return the type
   */
  @NotNull
  public String getContentType() {
    return "text/ideavim";
  }

  /**
   * Fetches the set of commands that can be used
   * on a text component that is using a model and
   * view produced by this kit.
   *
   * @return the set of actions
   */
  public Action[] getActions() {
    Action[] res = TextAction.augmentList(super.getActions(), this.exActions);
    if (logger.isDebugEnabled()) logger.debug("res.length=" + res.length);

    return res;
  }

  /**
   * Creates an uninitialized text storage model
   * that is appropriate for this type of editor.
   *
   * @return the model
   */
  @NotNull
  public Document createDefaultDocument() {
    return new ExDocument();
  }

  @Nullable
  private static KeyStroke convert(@NotNull ActionEvent event) {
    String cmd = event.getActionCommand();
    int mods = event.getModifiers();
    if (cmd != null && cmd.length() > 0) {
      char ch = cmd.charAt(0);
      if (ch < ' ') {
        if ((mods & KeyEvent.CTRL_MASK) != 0) {
          return KeyStroke.getKeyStroke(KeyEvent.VK_A + ch - 1, mods);
        }
      }
      else {
        return KeyStroke.getKeyStroke(new Character(ch), mods);
      }
    }

    return null;
  }

  static final String CancelEntry = "cancel-entry";
  static final String CompleteEntry = "complete-entry";
  static final String EscapeChar = "escape";
  static final String DeletePreviousChar = "delete-prev-char";
  static final String DeletePreviousWord = "delete-prev-word";
  static final String DeleteToCursor = "delete-to-cursor";
  static final String ToggleInsertReplace = "toggle-insert";
  static final String InsertRegister = "insert-register";
  static final String HistoryUp = "history-up";
  static final String HistoryDown = "history-down";
  static final String HistoryUpFilter = "history-up-filter";
  static final String HistoryDownFilter = "history-down-filter";
  static final String StartDigraph = "start-digraph";

  @NotNull private final Action[] exActions = new Action[]{
    new ExEditorKit.CancelEntryAction(),
    new ExEditorKit.CompleteEntryAction(),
    new ExEditorKit.EscapeCharAction(),
    new ExEditorKit.DeletePreviousCharAction(),
    new ExEditorKit.DeletePreviousWordAction(),
    new ExEditorKit.DeleteToCursorAction(),
    new ExEditorKit.HistoryUpAction(),
    new ExEditorKit.HistoryDownAction(),
    new ExEditorKit.HistoryUpFilterAction(),
    new ExEditorKit.HistoryDownFilterAction(),
    new ExEditorKit.ToggleInsertReplaceAction(),
    new ExEditorKit.StartDigraphAction(),
    new InsertRegisterAction(),
  };

  public static class DefaultExKeyHandler extends DefaultKeyTypedAction {
    public void actionPerformed(@NotNull ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      final Action currentAction = target.getCurrentAction();
      if (currentAction != null) {
        currentAction.actionPerformed(e);
      }
      else {
        KeyStroke key = convert(e);
        if (key != null) {
          final char c = key.getKeyChar();
          if (c > 0) {
            ActionEvent event = new ActionEvent(e.getSource(), e.getID(), "" + c, e.getWhen(), e.getModifiers());
            super.actionPerformed(event);
            target.saveLastEntry();
          }
        }
        else {
          super.actionPerformed(e);

          target.saveLastEntry();
        }
      }
    }
  }

  public interface MultiStepAction extends Action {
    void reset();
  }

  public static class HistoryUpAction extends TextAction {
    HistoryUpAction() {
      super(HistoryUp);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      target.selectHistory(true, false);
    }
  }

  public static class HistoryDownAction extends TextAction {
    HistoryDownAction() {
      super(HistoryDown);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      target.selectHistory(false, false);
    }
  }

  public static class HistoryUpFilterAction extends TextAction {
    HistoryUpFilterAction() {
      super(HistoryUpFilter);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      target.selectHistory(true, true);
    }
  }

  public static class HistoryDownFilterAction extends TextAction {
    HistoryDownFilterAction() {
      super(HistoryDownFilter);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      ExTextField target = (ExTextField)getTextComponent(actionEvent);
      target.selectHistory(false, true);
    }
  }

  public static class InsertRegisterAction extends TextAction implements MultiStepAction {
    private enum State {
      SKIP_CTRL_R,
      WAIT_REGISTER,
    }

    @NotNull private State state = State.SKIP_CTRL_R;

    InsertRegisterAction() {
      super(InsertRegister);
    }

    public void actionPerformed(@NotNull ActionEvent e) {
      final ExTextField target = (ExTextField)getTextComponent(e);
      final KeyStroke key = convert(e);
      if (key != null) {
        switch (state) {
          case SKIP_CTRL_R:
            state = State.WAIT_REGISTER;
            target.setCurrentAction(this, '\"');
            break;

          case WAIT_REGISTER:
            state = State.SKIP_CTRL_R;
            target.clearCurrentAction();
            final char c = key.getKeyChar();
            if (c != KeyEvent.CHAR_UNDEFINED) {
              final Register register = VimPlugin.getRegister().getRegister(c);
              if (register != null) {
                final String oldText = target.getActualText();
                final String text = register.getText();
                if (oldText != null && text != null) {
                  final int offset = target.getCaretPosition();
                  target.setText(oldText.substring(0, offset) + text + oldText.substring(offset));
                  target.setCaretPosition(offset + text.length());
                }
              }
            } else if ((key.getModifiers() & KeyEvent.CTRL_MASK) != 0 && key.getKeyCode() == KeyEvent.VK_C) {
              // Eat any unused keys, unless it's <C-C>, in which case forward on and cancel entry
              target.handleKey(key);
            }
        }
      }
    }

    @Override
    public void reset() {
      state = State.SKIP_CTRL_R;
    }
  }

  public static class CompleteEntryAction extends TextAction {
    CompleteEntryAction() {
      super(CompleteEntry);
    }

    public void actionPerformed(ActionEvent actionEvent) {
      logger.debug("complete entry");
      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

      // We send the <Enter> keystroke through the key handler rather than calling ProcessGroup#processExEntry directly.
      // We do this for a couple of reasons:
      // * The C mode mapping for ProcessExEntryAction handles the actual entry, and most importantly, it does so as a
      //   write action
      // * The key handler routines get the chance to clean up and reset state
      final ExTextField entry = ExEntryPanel.getInstance().getEntry();
      KeyHandler.getInstance().handleKey(entry.getEditor(), stroke, entry.getContext());
    }
  }

  public static class CancelEntryAction extends TextAction {
    CancelEntryAction() {
      super(CancelEntry);
    }

    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.cancel();
    }
  }

  public static class EscapeCharAction extends TextAction {
    EscapeCharAction() {
      super(EscapeChar);
    }

    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.escape();
    }
  }

  public static class DeletePreviousCharAction extends TextAction {
    DeletePreviousCharAction() {
      super(DeletePreviousChar);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.saveLastEntry();

      try {
        Document doc = target.getDocument();
        Caret caret = target.getCaret();
        int dot = caret.getDot();
        int mark = caret.getMark();
        if (dot != mark) {
          doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
        }
        else if (dot > 0) {
          int delChars = 1;

          if (dot > 1) {
            String dotChars = doc.getText(dot - 2, 2);
            char c0 = dotChars.charAt(0);
            char c1 = dotChars.charAt(1);

            if (c0 >= '\uD800' && c0 <= '\uDBFF' &&
                c1 >= '\uDC00' && c1 <= '\uDFFF') {
              delChars = 2;
            }
          }

          doc.remove(dot - delChars, delChars);
        }
        else if (dot == 0 && doc.getLength() == 0){
          target.cancel();
        }
      }
      catch (BadLocationException bl) {
        // ignore
      }
    }
  }

  public static class DeletePreviousWordAction extends TextAction {
    DeletePreviousWordAction() {
      super(DeletePreviousWord);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.saveLastEntry();

      Document doc = target.getDocument();
      Caret caret = target.getCaret();
      int offset = SearchHelper.findNextWord(target.getActualText(), caret.getDot(), target.getActualText().length(),
                                             -1, false, false);
      if (logger.isDebugEnabled()) logger.debug("offset=" + offset);
      try {
        int pos = caret.getDot();
        doc.remove(offset, pos - offset);
      }
      catch (BadLocationException ex) {
        // ignore
      }
    }
  }

  public static class DeleteToCursorAction extends TextAction {
    DeleteToCursorAction() {
      super(DeleteToCursor);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      ExTextField target = (ExTextField)getTextComponent(e);
      target.saveLastEntry();

      Document doc = target.getDocument();
      Caret caret = target.getCaret();
      try {
        doc.remove(0, caret.getDot());
      }
      catch (BadLocationException ex) {
        // ignore
      }
    }
  }

  public static class ToggleInsertReplaceAction extends TextAction {
    ToggleInsertReplaceAction() {
      super(ToggleInsertReplace);

      logger.debug("ToggleInsertReplaceAction()");
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      logger.debug("actionPerformed");
      ExTextField target = (ExTextField)getTextComponent(e);
      target.toggleInsertReplace();
    }
  }

  public static class StartDigraphAction extends TextAction implements MultiStepAction {
    @Nullable private DigraphSequence digraphSequence;

    StartDigraphAction() {
      super(StartDigraph);
    }

    public void actionPerformed(@NotNull ActionEvent e) {
      final ExTextField target = (ExTextField)getTextComponent(e);
      final KeyStroke key = convert(e);
      if (key != null && digraphSequence != null) {
        DigraphSequence.DigraphResult res = digraphSequence.processKey(key, target.getEditor());
        switch (res.getResult()) {
          case DigraphSequence.DigraphResult.RES_OK:
            target.setCurrentActionPromptCharacter(res.getPromptCharacter());
            break;

          case DigraphSequence.DigraphResult.RES_BAD:
            target.clearCurrentAction();
            // Eat the character, unless it's <C-C>, in which case, forward on and cancel entry. Note that at some point
            // we should support input of control characters
            if ((key.getModifiers() & KeyEvent.CTRL_MASK) != 0 && key.getKeyCode() == KeyEvent.VK_C) {
              target.handleKey(key);
            }
            break;

          case DigraphSequence.DigraphResult.RES_DONE:
            final KeyStroke digraph = res.getStroke();
            digraphSequence = null;
            target.clearCurrentAction();
            if (digraph != null) {
              target.handleKey(digraph);
            }
            break;
        }
      }
      else if (key != null && DigraphSequence.isDigraphStart(key)) {
        digraphSequence = new DigraphSequence();
        DigraphSequence.DigraphResult res = digraphSequence.processKey(key, target.getEditor());
        target.setCurrentAction(this, res.getPromptCharacter());
      }
    }

    @Override
    public void reset() {
      digraphSequence = null;
    }
  }

  private static ExEditorKit instance;

  private static final Logger logger = Logger.getInstance(ExEditorKit.class.getName());
}

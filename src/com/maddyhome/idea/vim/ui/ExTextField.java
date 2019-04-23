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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.JBUI;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.group.HistoryGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.List;

import static java.lang.Math.max;

/**
 * Provides a custom keymap for the text field. The keymap is the VIM Ex command keymapping
 */
public class ExTextField extends JTextField {

  ExTextField() {
    CommandLineCaret caret = new CommandLineCaret();
    caret.setBlinkRate(getCaret().getBlinkRate());
    setCaret(caret);
    setNormalModeCaret();

    addCaretListener(e -> resetCaret());

    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        setCaretPosition(getDocument().getLength());
      }
    });
  }

  // Minimize margins and insets. These get added to the default margins in the UI class that we can't override.
  // (I.e. DarculaTextFieldUI#getDefaultMargins, MacIntelliJTextFieldUI#getDefaultMargin, WinIntelliJTextFieldUI#getDefaultMargin)
  // This is an attempt to mitigate the gap in ExEntryPanel between the label (':', '/', '?') and the text field.
  // See VIM-1485
  @Override
  public Insets getMargin() {
    return JBUI.emptyInsets();
  }

  @Override
  public Insets getInsets() {
    return JBUI.emptyInsets();
  }

  // Called when the LAF is changed, but only if the control is visible
  @Override
  public void updateUI() {
    super.updateUI();

    setBorder(null);

    // Do not override getActions() method, because it is has side effect: propogates these actions to defaults.
    final Action[] actions = ExEditorKit.getInstance().getActions();
    final ActionMap actionMap = getActionMap();
    for (Action a : actions) {
      actionMap.put(a.getValue(Action.NAME), a);
      //System.out.println("  " + a.getValue(Action.NAME));
    }

    setInputMap(WHEN_FOCUSED, new InputMap());
    Keymap map = addKeymap("ex", getKeymap());
    loadKeymap(map, ExKeyBindings.getBindings(), actions);
    map.setDefaultAction(new ExEditorKit.DefaultExKeyHandler());
    setKeymap(map);
  }

  void setType(@NotNull String type) {
    String hkey = null;
    switch (type.charAt(0)) {
      case '/':
      case '?':
        hkey = HistoryGroup.SEARCH;
        break;
      case ':':
        hkey = HistoryGroup.COMMAND;
        break;
    }

    if (hkey != null) {
      history = VimPlugin.getHistory().getEntries(hkey, 0, 0);
      histIndex = history.size();
    }
  }

  void saveLastEntry() {
    lastEntry = getText();
  }

  void selectHistory(boolean isUp, boolean filter) {
    int dir = isUp ? -1 : 1;
    if (histIndex + dir < 0 || histIndex + dir > history.size()) {
      VimPlugin.indicateError();

      return;
    }

    if (filter) {
      for (int i = histIndex + dir; i >= 0 && i <= history.size(); i += dir) {
        String txt;
        if (i == history.size()) {
          txt = lastEntry;
        }
        else {
          HistoryGroup.HistoryEntry entry = history.get(i);
          txt = entry.getEntry();
        }

        if (txt.startsWith(lastEntry)) {
          updateText(txt);
          histIndex = i;

          return;
        }
      }

      VimPlugin.indicateError();
    }
    else {
      histIndex = histIndex + dir;
      String txt;
      if (histIndex == history.size()) {
        txt = lastEntry;
      }
      else {
        HistoryGroup.HistoryEntry entry = history.get(histIndex);
        txt = entry.getEntry();
      }

      updateText(txt);
    }
  }

  private void updateText(String string) {
    super.setText(string);
  }

  public void setText(String string) {
    super.setText(string);

    saveLastEntry();
  }

  void setEditor(Editor editor, DataContext context) {
    this.editor = editor;
    this.context = context;
    String disposeKey = vimExTextFieldDisposeKey + editor.hashCode();
    Project project = editor.getProject();
    if (Disposer.get(disposeKey) == null && project != null) {
      Disposer.register(project, () -> {
        this.editor = null;
        this.context = null;
      }, disposeKey);
    }
  }

  public Editor getEditor() {
    return editor;
  }

  public DataContext getContext() {
    return context;
  }

  public void handleKey(@NotNull KeyStroke stroke) {
    if (logger.isDebugEnabled()) logger.debug("stroke=" + stroke);
    final char keyChar = stroke.getKeyChar();
    char c = keyChar;
    final int modifiers = stroke.getModifiers();
    final int keyCode = stroke.getKeyCode();
    if ((modifiers & KeyEvent.CTRL_MASK) != 0) {
      final int codePoint = keyCode - KeyEvent.VK_A + 1;
      if (codePoint > 0) {
        c = Character.toChars(codePoint)[0];
      }
    }
    KeyEvent event = new KeyEvent(this, keyChar != KeyEvent.CHAR_UNDEFINED ? KeyEvent.KEY_TYPED :
                                        (stroke.isOnKeyRelease() ? KeyEvent.KEY_RELEASED : KeyEvent.KEY_PRESSED),
                                  (new Date()).getTime(), modifiers, keyCode, c);

    super.processKeyEvent(event);
  }

  protected void processKeyEvent(KeyEvent e) {
    if (logger.isDebugEnabled()) logger.debug("key=" + e);
    super.processKeyEvent(e);
  }

  /**
   * Creates the default implementation of the model
   * to be used at construction if one isn't explicitly
   * given.  An instance of <code>PlainDocument</code> is returned.
   *
   * @return the default model implementation
   */
  @NotNull
  protected Document createDefaultModel() {
    return new ExDocument();
  }

  public void escape() {
    if (currentAction != null) {
      currentAction = null;
    }
    else {
      VimPlugin.getProcess().cancelExEntry(editor, context);
    }
  }

  void setCurrentAction(@Nullable Action action) {
    this.currentAction = action;
  }

  @Nullable
  Action getCurrentAction() {
    return currentAction;
  }

  void setInsertMode() {
    ExDocument doc = (ExDocument)getDocument();
    if (doc.isOverwrite()) {
      doc.toggleInsertReplace();
    }
    resetCaret();
  }

  void toggleInsertReplace() {
    ExDocument doc = (ExDocument)getDocument();
    doc.toggleInsertReplace();
    resetCaret();
  }

  private void resetCaret() {
    if (getCaretPosition() == getText().length()) {
      setNormalModeCaret();
    }
    else {
      ExDocument doc = (ExDocument)getDocument();
      if (doc.isOverwrite()) {
        setReplaceModeCaret();
      }
      else {
        setInsertModeCaret();
      }
    }
  }

  // The default cursor shapes for command line are:
  // 'c' command-line normal is block
  // 'ci' command-line insert is ver25
  // 'cr' command-line replace is hor20
  // see :help 'guicursor'
  // Note that we can't easily support guicursor because we don't have arbitrary control over the IntelliJ editor caret
  private void setNormalModeCaret() {
    CommandLineCaret caret = (CommandLineCaret) getCaret();
    caret.setBlockMode();
  }

  private void setInsertModeCaret() {
    CommandLineCaret caret = (CommandLineCaret) getCaret();
    caret.setMode(CommandLineCaret.CaretMode.VER, 25);
  }

  private void setReplaceModeCaret() {
    CommandLineCaret caret = (CommandLineCaret) getCaret();
    caret.setMode(CommandLineCaret.CaretMode.HOR, 20);
  }

  private static class CommandLineCaret extends DefaultCaret {

    private CaretMode mode;
    private int blockPercentage = 100;

    public enum CaretMode {
      BLOCK,
      VER,
      HOR
    }

    void setBlockMode() {
      setMode(CaretMode.BLOCK, 100);
    }

    void setMode(CaretMode mode, int blockPercentage) {

      // Make sure damage gets updated for the old and new shape so the flashing works correctly
      updateDamage();
      this.mode = mode;
      this.blockPercentage = blockPercentage;
      updateDamage();
    }

    private void updateDamage() {
      try {
        Rectangle r = getComponent().getUI().modelToView(getComponent(), getDot(), getDotBias());
        damage(r);
      }
      catch(BadLocationException e) {
        // ignore
      }
    }

    @Override
    public void paint(Graphics g) {
      if (!isVisible())
        return;

      try {
        final JTextComponent component = getComponent();
        final Color color = g.getColor();

        g.setColor(component.getBackground());
        g.setXORMode(component.getCaretColor());

        // We have to use the deprecated version because we still support 1.8
        final Rectangle r = component.getUI().modelToView(component, getDot(), getDotBias());
        FontMetrics fm = g.getFontMetrics();
        final int blockHeight = fm.getHeight();
        r.setBounds(r.x, r.y, getBlockWidth(fm), getBlockHeight(blockHeight));
        g.fillRect(r.x, r.y + blockHeight - r.height, r.width, r.height);
        g.setPaintMode();
        g.setColor(color);
      }
      catch (BadLocationException e) {
        // ignore
      }
    }

    protected synchronized void damage(Rectangle r) {
      if (r != null) {
        JTextComponent component = getComponent();
        Font font = component.getFont();
        FontMetrics fm = component.getFontMetrics(font);
        final int blockHeight = fm.getHeight();
        width = getBlockWidth(fm);
        height = getBlockHeight(blockHeight);
        x = r.x;
        y = r.y + blockHeight - height;
        repaint();
      }
    }

    private int getBlockWidth(FontMetrics fm) {
      if (mode == CaretMode.VER) {
        // Don't show a proportional width of a proportional font
        final int fullWidth = fm.charWidth('o');
        return max(1, fullWidth * blockPercentage / 100);
      }

      final char c = ((ExDocument)getComponent().getDocument()).getCharacter(getComponent().getCaretPosition());
      return fm.charWidth(c);
    }

    private int getBlockHeight(int fullHeight) {
      if (mode == CaretMode.HOR) {
        return max(1, fullHeight * blockPercentage / 100);
      }
      return fullHeight;
    }
  }

  private Editor editor;
  private DataContext context;
  private String lastEntry;
  private List<HistoryGroup.HistoryEntry> history;
  private int histIndex = 0;
  @Nullable private Action currentAction;

  private static final String vimExTextFieldDisposeKey = "vimExTextFieldDisposeKey";
  private static final Logger logger = Logger.getInstance(ExTextField.class.getName());
}

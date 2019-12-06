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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Provides a custom keymap for the text field. The keymap is the VIM Ex command keymapping
 */
public class ExTextField extends JTextField {

  ExTextField() {
    // We need to store this in a field, because we can't trust getCaret(), as it will return an instance of
    // ComposedTextCaret when working with dead keys or input methods
    caret = new CommandLineCaret();
    caret.setBlinkRate(getCaret().getBlinkRate());
    setCaret(caret);
    setNormalModeCaret();

    addCaretListener(e -> resetCaret());
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        // If we're in the middle of an action (e.g. entering a register to paste, or inserting a digraph), cancel it if
        // the mouse is clicked anywhere. Vim's behavior is to use the mouse click as an event, which can lead to
        // something like : !%!C, which I don't believe is documented, or useful
        if (currentAction != null) {
          clearCurrentAction();
        }
        super.mouseClicked(e);
      }
    });
  }

  void reset() {
    clearCurrentAction();
    setInsertMode();
  }

  void deactivate() {
    clearCurrentAction();
    editor = null;
    context = null;
  }

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
    // Override the default look and feel specific UI so we can have a completely borderless and margin-less text field.
    // (See TextFieldWithPopupHandlerUI#getDefaultMargins and derived classes). This allows us to draw the text field
    // directly next to the label
    setUI(new BasicTextFieldUI());
    invalidate();

    setBorder(null);

    // Do not override getActions() method, because it is has side effect: propagates these actions to defaults.
    final Action[] actions = ExEditorKit.getInstance().getActions();
    final ActionMap actionMap = getActionMap();
    for (Action a : actions) {
      actionMap.put(a.getValue(Action.NAME), a);
      //System.out.println("  " + a.getValue(Action.NAME));
    }

    setInputMap(WHEN_FOCUSED, new InputMap());
    Keymap map = addKeymap("ex", getKeymap());
    loadKeymap(map, ExKeyBindings.INSTANCE.getBindings(), actions);
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

  /**
   * Stores the current text for use in filtering history. Required for scrolling through multiple history entries
   *
   * Called whenever the text is changed, either by typing, or by special characters altering the text (e.g. Delete)
   */
  void saveLastEntry() {
    lastEntry = super.getText();
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

  @Override
  public void setText(String string) {
    super.setText(string);

    saveLastEntry();
  }

  /**
   * @deprecated Use getActualText()
   * Using this method can include prompt characters used when entering digraphs or register text
   */
  @Override
  @Deprecated
  public String getText() {
    return super.getText();
  }

  @NotNull
  String getActualText() {
    if (actualText != null) {
      return actualText;
    }
    final String text = super.getText();
    return text == null ? "" : text;
  }

  void setEditor(@NotNull Editor editor, DataContext context) {
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

    // Make sure the current action sees any subsequent keystrokes, and they're not processed by Swing's action system.
    // Note that this will only handle simple characters and any control characters that are already registered against
    // ExShortcutKeyAction - any other control characters will can be "stolen" by other IDE actions.
    // If we need to capture ANY subsequent keystroke (e.g. for ^V<Tab>, or to stop the Swing standard <C-A> going to
    // start of line), we should replace ExShortcutAction with a dispatcher registered with IdeEventQueue#addDispatcher.
    // This gets called for ALL events, before the IDE starts to process key events for the action system. We can add a
    // dispatcher that checks that the plugin is enabled, checks that the component with the focus is ExTextField,
    // dispatch to ExEntryPanel#handleKey and if it's processed, mark the event as consumed.
    if (currentAction != null) {
      currentAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "" + c, modifiers));
    }
    else {
      KeyEvent event = new KeyEvent(this, keyChar != KeyEvent.CHAR_UNDEFINED ? KeyEvent.KEY_TYPED :
        (stroke.isOnKeyRelease() ? KeyEvent.KEY_RELEASED : KeyEvent.KEY_PRESSED),
        (new Date()).getTime(), modifiers, keyCode, c);

      super.processKeyEvent(event);
    }
  }

  @Override
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
  @Override
  @NotNull
  protected Document createDefaultModel() {
    return new ExDocument();
  }

  /**
   * Cancels current action, if there is one. If not, cancels entry.
   */
  void escape() {
    if (currentAction != null) {
      clearCurrentAction();
    }
    else {
      cancel();
    }
  }

  /**
   * Cancels entry, including any current action.
   */
  void cancel() {
    clearCurrentAction();
    VimPlugin.getProcess().cancelExEntry(editor, true);
  }

  void setCurrentAction(@NotNull ExEditorKit.MultiStepAction action, char pendingIndicator) {
    this.currentAction = action;
    setCurrentActionPromptCharacter(pendingIndicator);
  }

  void clearCurrentAction() {
    if (currentAction != null) {
      currentAction.reset();
    }
    currentAction = null;
    clearCurrentActionPromptCharacter();
  }

  void setCurrentActionPromptCharacter(char promptCharacter) {
    actualText = removePromptCharacter();
    this.currentActionPromptCharacter = promptCharacter;
    currentActionPromptCharacterOffset = currentActionPromptCharacterOffset == -1 ? getCaretPosition() : currentActionPromptCharacterOffset;
    StringBuilder sb = new StringBuilder(actualText);
    sb.insert(currentActionPromptCharacterOffset, currentActionPromptCharacter);
    updateText(sb.toString());
    setCaretPosition(currentActionPromptCharacterOffset);
  }

  private void clearCurrentActionPromptCharacter() {
    final int offset = getCaretPosition();
    final String text = removePromptCharacter();
    updateText(text);
    setCaretPosition(min(offset, text.length()));
    currentActionPromptCharacter = '\0';
    currentActionPromptCharacterOffset = -1;
    actualText = null;
  }

  private String removePromptCharacter() {
    return currentActionPromptCharacterOffset == -1
      ? super.getText()
      : StringsKt.removeRange(super.getText(), currentActionPromptCharacterOffset, currentActionPromptCharacterOffset + 1).toString();
  }

  @Nullable
  Action getCurrentAction() {
    return currentAction;
  }

  private void setInsertMode() {
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
    if (getCaretPosition() == super.getText().length() || currentActionPromptCharacterOffset == super.getText().length() - 1) {
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
    caret.setBlockMode();
  }

  private void setInsertModeCaret() {
    caret.setMode(CommandLineCaret.CaretMode.VER, 25);
  }

  private void setReplaceModeCaret() {
    caret.setMode(CommandLineCaret.CaretMode.HOR, 20);
  }

  private static class CommandLineCaret extends DefaultCaret {

    private CaretMode mode;
    private int blockPercentage = 100;
    private int lastBlinkRate = 0;
    private boolean hasFocus;

    public enum CaretMode {
      BLOCK,
      VER,
      HOR
    }

    void setBlockMode() {
      setMode(CaretMode.BLOCK, 100);
    }

    void setMode(CaretMode mode, int blockPercentage) {
      if (this.mode == mode && this.blockPercentage == blockPercentage) {
        return;
      }

      // Hide the current caret and redraw without it. Then make the new caret visible, but only if it was already
      // (logically) visible/active. Always making it visible can start the flasher timer unnecessarily.
      final boolean active = isActive();
      if (isVisible()) {
        setVisible(false);
      }
      this.mode = mode;
      this.blockPercentage = blockPercentage;
      if (active) {
        setVisible(true);
      }
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
    public void focusGained(FocusEvent e) {
      if (lastBlinkRate != 0) {
        setBlinkRate(lastBlinkRate);
        lastBlinkRate = 0;
      }
      super.focusGained(e);
      updateDamage();
      hasFocus = true;
    }

    @Override
    public void focusLost(FocusEvent e) {
      hasFocus = false;
      lastBlinkRate = getBlinkRate();
      setBlinkRate(0);
      // We might be losing focus while the cursor is flashing, and is currently not visible
      setVisible(true);
      updateDamage();
    }

    @Override
    public void paint(Graphics g) {
      if (!isVisible()) return;

      try {
        final JTextComponent component = getComponent();
        g.setColor(component.getCaretColor());

        // We have to use the deprecated version because we still support 1.8
        final Rectangle r = component.getUI().modelToView(component, getDot(), getDotBias());
        FontMetrics fm = g.getFontMetrics();
        final int boundsHeight = fm.getHeight();
        if (!hasFocus) {
          r.setBounds(r.x, r.y, getCaretWidth(fm, 100), boundsHeight);
          g.drawRect(r.x, r.y, r.width, r.height);
        }
        else {
          r.setBounds(r.x, r.y, getCaretWidth(fm, blockPercentage), getBlockHeight(boundsHeight));
          g.fillRect(r.x, r.y + boundsHeight - r.height, r.width, r.height);
        }
      }
      catch (BadLocationException e) {
        // ignore
      }
    }

    @Override
    protected synchronized void damage(Rectangle r) {
      if (r != null) {
        JTextComponent component = getComponent();
        Font font = component.getFont();
        FontMetrics fm = component.getFontMetrics(font);
        final int blockHeight = fm.getHeight();
        if (!hasFocus) {
          width = this.getCaretWidth(fm, 100);
          height = blockHeight;
        }
        else {
          width = this.getCaretWidth(fm, blockPercentage);
          height = getBlockHeight(blockHeight);
        }
        x = r.x;
        y = r.y + blockHeight - height;
        repaint();
      }
    }

    private int getCaretWidth(FontMetrics fm, int widthPercentage) {
      if (mode == CaretMode.VER) {
        // Don't show a proportional width of a proportional font
        final int fullWidth = fm.charWidth('o');
        return max(1, fullWidth * widthPercentage / 100);
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

  @TestOnly
  public String getCaretShape() {
    CommandLineCaret caret = (CommandLineCaret) getCaret();
    return String.format("%s %d", caret.mode, caret.blockPercentage);
  }

  private Editor editor;
  private DataContext context;
  private CommandLineCaret caret;
  private String lastEntry;
  private String actualText;
  private List<HistoryGroup.HistoryEntry> history;
  private int histIndex = 0;
  @Nullable private ExEditorKit.MultiStepAction currentAction;
  private char currentActionPromptCharacter;
  private int currentActionPromptCharacterOffset = -1;

  private static final String vimExTextFieldDisposeKey = "vimExTextFieldDisposeKey";
  private static final Logger logger = Logger.getInstance(ExTextField.class.getName());
}

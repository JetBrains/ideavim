/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.ui.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ui.JBUI;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.VimProjectService;
import com.maddyhome.idea.vim.group.HistoryGroup;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.option.GuiCursorAttributes;
import com.maddyhome.idea.vim.option.GuiCursorMode;
import com.maddyhome.idea.vim.option.GuiCursorType;
import com.maddyhome.idea.vim.option.OptionsManager;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.List;

import static java.lang.Math.*;

/**
 * Provides a custom keymap for the text field. The keymap is the VIM Ex command keymapping
 */
public class ExTextField extends JTextField {

  public static final @NonNls String KEYMAP_NAME = "ex";

  public boolean useHandleKeyFromEx = true;

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
    final Action[] actions = ExEditorKit.INSTANCE.getActions();
    final ActionMap actionMap = getActionMap();
    for (Action a : actions) {
      actionMap.put(a.getValue(Action.NAME), a);
      //System.out.println("  " + a.getValue(Action.NAME));
    }

    setInputMap(WHEN_FOCUSED, new InputMap());
    Keymap map = addKeymap(KEYMAP_NAME, getKeymap());
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
      histIndex += dir;
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

  // fix https://youtrack.jetbrains.com/issue/VIM-570
  private void resetFont(String string) {
    super.setFont(UiHelper.selectFont(string));
  }

  private void updateText(String string) {
    super.setText(string);

    resetFont(string);
  }

  @Override
  public void setText(String string) {
    super.setText(string);

    saveLastEntry();
    resetFont(string);
  }

  @NotNull
  String getActualText() {
    if (actualText != null) {
      return actualText;
    }
    final String text = super.getText();
    return text == null ? "" : text;
  }

  // I'm not sure how to deal with these dispose issues and deprecations
  @SuppressWarnings("deprecation")
  void setEditor(@NotNull Editor editor, DataContext context) {
    this.editor = editor;
    this.context = context;
    String disposeKey = vimExTextFieldDisposeKey + editor.hashCode();
    Project project = editor.getProject();
    if (Disposer.get(disposeKey) == null && project != null) {
      VimProjectService parentDisposable = VimProjectService.getInstance(project);
      Disposer.register(parentDisposable, () -> {
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
    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
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
      currentAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, String.valueOf(c), modifiers));
    }
    else {
      KeyEvent event = new KeyEvent(this, keyChar != KeyEvent.CHAR_UNDEFINED ? KeyEvent.KEY_TYPED :
        (stroke.isOnKeyRelease() ? KeyEvent.KEY_RELEASED : KeyEvent.KEY_PRESSED),
        (new Date()).getTime(), modifiers, keyCode, c);

      useHandleKeyFromEx = false;
      try {
        super.processKeyEvent(event);
      }finally {
        useHandleKeyFromEx = true;
      }
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
  protected @NotNull Document createDefaultModel() {
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

  public void setCurrentAction(@NotNull MultiStepAction action, char pendingIndicator) {
    this.currentAction = action;
    setCurrentActionPromptCharacter(pendingIndicator);
  }

  public void clearCurrentAction() {
    if (currentAction != null) {
      currentAction.reset();
    }
    currentAction = null;
    clearCurrentActionPromptCharacter();
  }

  /**
   * Text to show while composing a digraph or inserting a literal or register
   *
   * The prompt character is inserted directly into the text of the text field, rather than drawn over the top of the
   * current character. When the action has been completed, the new character(s) are either inserted or overwritten,
   * depending on the insert/overwrite status of the text field. This mimics Vim's behaviour.
   *
   * @param promptCharacter The character to show as prompt
   */
  public void setCurrentActionPromptCharacter(char promptCharacter) {
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
  private void setNormalModeCaret() {
    caret.setAttributes(OptionsManager.INSTANCE.getGuicursor().getAttributes(GuiCursorMode.CMD_LINE));
  }

  private void setInsertModeCaret() {
    caret.setAttributes(OptionsManager.INSTANCE.getGuicursor().getAttributes(GuiCursorMode.CMD_LINE_INSERT));
  }

  private void setReplaceModeCaret() {
    caret.setAttributes(OptionsManager.INSTANCE.getGuicursor().getAttributes(GuiCursorMode.CMD_LINE_REPLACE));
  }

  private static class CommandLineCaret extends DefaultCaret {

    private GuiCursorType mode;
    private int thickness = 100;
    private int lastBlinkRate = 0;
    private boolean hasFocus;

    public void setAttributes(GuiCursorAttributes attributes) {
      final boolean active = isActive();

      // Hide the currently visible caret
      if (isVisible()) {
        setVisible(false);
      }

      mode = attributes.getType();
      thickness = mode == GuiCursorType.BLOCK ? 100 : attributes.getThickness();

      // Make sure the caret is visible, but only if we're active, otherwise we'll kick off the flasher timer unnecessarily
      if (active) {
        setVisible(true);
      }
    }

    @Override
    public void focusGained(FocusEvent e) {
      if (lastBlinkRate != 0) {
        setBlinkRate(lastBlinkRate);
        lastBlinkRate = 0;
      }
      super.focusGained(e);
      repaint();
      hasFocus = true;
    }

    @Override
    public void focusLost(FocusEvent e) {
      // We don't call super.focusLost, which would hide the caret
      hasFocus = false;
      lastBlinkRate = getBlinkRate();
      setBlinkRate(0);
      // Make sure the box caret is visible. If we're flashing, this might be false
      setVisible(true);
      repaint();
    }

    @Override
    public void paint(Graphics g) {
      if (!isVisible()) return;

      // Take a copy of the graphics, so we can mess around with it without having to reset after
      final Graphics2D g2d = (Graphics2D) g.create();
      try {
        final JTextComponent component = getComponent();

        g2d.setColor(component.getCaretColor());
        g2d.setXORMode(component.getBackground());

        final Rectangle2D r = modelToView(getDot());
        if (r == null) {
          return;
        }

        // Make sure not to use the saved bounds! There is no guarantee that damage() has been called first, especially
        // when the caret has not yet been moved or changed
        final FontMetrics fm = component.getFontMetrics(component.getFont());
        if (!hasFocus) {
          final float outlineThickness = (float) PaintUtil.alignToInt(1.0, g2d);
          final double caretWidth = getCaretWidth(fm, r.getX(), 100);
          final Area area = new Area(new Rectangle2D.Double(r.getX(), r.getY(), caretWidth, r.getHeight()));
          area.subtract(new Area(new Rectangle2D.Double(r.getX() + outlineThickness, r.getY() + outlineThickness, caretWidth - (2 * outlineThickness), r.getHeight() - (2 * outlineThickness))));
          g2d.fill(area);
        }
        else {
          final double caretHeight = getCaretHeight(r.getHeight());
          final double caretWidth = getCaretWidth(fm, r.getX(), thickness);
          g2d.fill(new Rectangle2D.Double(r.getX(), r.getY() + r.getHeight() - caretHeight, caretWidth, caretHeight));
        }
      }
      finally {
        g2d.dispose();
      }
    }

    /**
     * Updates the bounds of the caret and repaints those bounds.
     *
     * This method is not guaranteed to be called before paint(). The bounds are for use by repaint().
     *
     * @param r The current location of the caret, usually provided by MapToView. The x and y appear to be the upper
     *          left of the character position. The height appears to be correct, but the width is not the character
     *          width. We also get an int Rectangle, which might not match the float Rectangle we use to draw the caret
     */
    @Override
    protected synchronized void damage(Rectangle r) {
      if (r != null) {

        // Always set the bounds to the full character grid, so that we are sure we will always erase any old caret.
        // Note that we get an int Rectangle, while we draw with a float Rectangle. The x value is fine as it will
        // round down when converting. The width is rounded up, but should also include any fraction part from x, so we
        // add one.
        final FontMetrics fm = getComponent().getFontMetrics(getComponent().getFont());
        x = r.x;
        y = r.y;
        width = (int)ceil(getCaretWidth(fm, r.x, 100)) + 1;
        height = r.height;
        repaint();
      }
    }

    // [VERSION UPDATE] 203+ Use modelToView2D, which will return a float rect which positions the caret better
    // Java 9+
    private @Nullable Rectangle2D modelToView(int dot) {
      if (dot > getComponent().getDocument().getLength()) {
        return null;
      }

      try {
        return getComponent().getUI().modelToView(getComponent(), dot, getDotBias());
      }
      catch (BadLocationException e) {
        return null;
      }
    }

    private double getCaretWidth(FontMetrics fm, double dotX, int widthPercentage) {
      // Caret width is based on the distance to the next character. This isn't necessarily the same as the character
      // width. E.g. when using float coordinates, the width of a grid is 8.4, while the character width is only 8. This
      // would give us a caret that is not wide enough
      double width;
      final Rectangle2D r = modelToView(getDot() + 1);
      if (r != null) {
        // [VERSION UPDATE] 203+ Remove this +1. It's a fudge factor because we're working with integers
        // When we use modelToView2D to get accurate measurements this won't be required. E.g. width can be 8.4, with a
        // starting x of 8.4, which would put the right hand edge at 16.8. Because everything is rounded down, we get 16
        // So we add one
        width = r.getX() - dotX + 1;
      }
      else {
        char c = ' ';
        try {
          if (getDot() < getComponent().getDocument().getLength()) {
            c = getComponent().getText(getDot(), 1).charAt(0);
          }
        }
        catch (BadLocationException e) {
          // Ignore
        }
        width = fm.charWidth(c);
      }

      return mode == GuiCursorType.VER ? max(1.0, width * widthPercentage / 100) : width;
    }

    private double getCaretHeight(double fullHeight) {
      if (mode == GuiCursorType.HOR) {
        return max(1.0, fullHeight * thickness / 100.0);
      }
      return fullHeight;
    }
  }

  @TestOnly
  public @NonNls String getCaretShape() {
    CommandLineCaret caret = (CommandLineCaret) getCaret();
    return String.format("%s %d", caret.mode, caret.thickness);
  }

  private Editor editor;
  private DataContext context;
  private final CommandLineCaret caret;
  private String lastEntry;
  private String actualText;
  private List<HistoryGroup.HistoryEntry> history;
  private int histIndex = 0;
  private @Nullable MultiStepAction currentAction;
  private char currentActionPromptCharacter;
  private int currentActionPromptCharacterOffset = -1;

  private static final String vimExTextFieldDisposeKey = "vimExTextFieldDisposeKey";
  private static final Logger logger = Logger.getInstance(ExTextField.class.getName());
}

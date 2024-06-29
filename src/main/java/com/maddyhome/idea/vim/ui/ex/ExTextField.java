/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ui.JBUI;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.VimCommandLine;
import com.maddyhome.idea.vim.api.VimCommandLineCaret;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.history.HistoryConstants;
import com.maddyhome.idea.vim.history.HistoryEntry;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.helpers.GuiCursorAttributes;
import com.maddyhome.idea.vim.options.helpers.GuiCursorMode;
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper;
import com.maddyhome.idea.vim.options.helpers.GuiCursorType;
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

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
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
    ExEntryPanel.getInstance().setEditor(null);
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
    String hkey = switch (type.charAt(0)) {
      case '/', '?' -> HistoryConstants.SEARCH;
      case ':' -> HistoryConstants.COMMAND;
      default -> null;
    };

    if (hkey != null) {
      history = VimPlugin.getHistory().getEntries(hkey, 0, 0);
      histIndex = history.size();
    }
  }

  /**
   * Stores the current text for use in filtering history. Required for scrolling through multiple history entries
   * <p>
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
          HistoryEntry entry = history.get(i);
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
        HistoryEntry entry = history.get(histIndex);
        txt = entry.getEntry();
      }

      updateText(txt);
    }
  }

  void updateText(String string) {
    super.setText(string);
    setFontToJField(string);
  }

  @Override
  public void setText(String string) {
    super.setText(string);

    saveLastEntry();
    setFontToJField(string);
  }

  // VIM-570
  private void setFontToJField(String stringToDisplay) {
    super.setFont(UiHelper.selectEditorFont(getEditor(), stringToDisplay));
  }

  void setEditor(@NotNull Editor editor, DataContext context) {
    this.context = context;
    ExEntryPanel.getInstance().setEditor(editor);
  }

  public @Nullable Editor getEditor() {
    return ExEntryPanel.getInstance().getEditor();
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
    Editor editor = ExEntryPanel.instance.getEditor();
    if (editor != null) {
      VimPlugin.getProcess().cancelExEntry(new IjVimEditor(editor), true, true);
    }
  }

  public void clearCurrentAction() {
    if (currentAction != null) {
      currentAction.reset();
    }
    currentAction = null;
    VimCommandLine commandLine = injector.getCommandLine().getActiveCommandLine();
    if (commandLine != null) commandLine.clearPromptCharacter();
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

    // Hide/show the caret so its new shape is immediately visible
    caret.setVisible(false);
    resetCaret();
    caret.setVisible(true);
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
    caret.setAttributes(GuiCursorOptionHelper.INSTANCE.getAttributes(GuiCursorMode.CMD_LINE));
  }

  private void setInsertModeCaret() {
    caret.setAttributes(GuiCursorOptionHelper.INSTANCE.getAttributes(GuiCursorMode.CMD_LINE_INSERT));
  }

  private void setReplaceModeCaret() {
    caret.setAttributes(GuiCursorOptionHelper.INSTANCE.getAttributes(GuiCursorMode.CMD_LINE_REPLACE));
  }

  private static class CommandLineCaret extends DefaultCaret implements VimCommandLineCaret {

    private GuiCursorType mode;
    private int thickness = 100;
    private int lastBlinkRate = 0;
    private boolean hasFocus;

    public void setAttributes(GuiCursorAttributes attributes) {

      // Note: do not call anything that causes a layout in this method! E.g. setVisible. This method is used as a
      // callback whenever the caret moves, and causing a layout at this point can cause issues such as an infinite
      // loop in the layout algorithm with multi-width characters such as emoji or non-Latin characters (I don't know
      // why the layout algorithm gets stuck, but we can easily avoid it)
      // See VIM-2562

      mode = attributes.getType();
      thickness = mode == GuiCursorType.BLOCK ? 100 : attributes.getThickness();
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

        g2d.setColor(component.getBackground());
        g2d.setXORMode(component.getCaretColor());

        final Rectangle2D r = modelToView(getDot());
        if (r == null) {
          return;
        }

        // Make sure our clip region is still up to date. It might get out of sync due to the IDE scale changing.
        // (Note that the DefaultCaret class makes this check)
        if (width > 0 && height > 0 && !contains(r)) {
          Rectangle clip = g2d.getClipBounds();
          if (clip != null && !clip.contains(this)) {
            repaint();
          }
          damage(r.getBounds());
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
          Double rect = new Double(r.getX(), r.getY() + r.getHeight() - caretHeight, caretWidth, caretHeight);
          g2d.fill(rect);
        }
      }
      finally {
        g2d.dispose();
      }
    }

    /**
     * Updates the bounds of the caret and repaints those bounds.
     * <p>
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

    private @Nullable Rectangle2D modelToView(int dot) {
      if (dot > getComponent().getDocument().getLength()) {
        return null;
      }

      try {
        return getComponent().getUI().modelToView2D(getComponent(), dot, getDotBias());
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
        width = r.getX() - dotX;
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

    @Override
    public int getOffset() {
      return getDot();
    }

    @Override
    public void setOffset(int i) {
      setDot(i);
    }
  }

  @TestOnly
  public @NonNls String getCaretShape() {
    CommandLineCaret caret = (CommandLineCaret) getCaret();
    return String.format("%s %d", caret.mode, caret.thickness);
  }

  private DataContext context;
  private final CommandLineCaret caret;
  private String lastEntry;
  private List<HistoryEntry> history;
  private int histIndex = 0;
  private @Nullable MultiStepAction currentAction;
  int currentActionPromptCharacterOffset = -1;

  private static final Logger logger = Logger.getInstance(ExTextField.class.getName());
}

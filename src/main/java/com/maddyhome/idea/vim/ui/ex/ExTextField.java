/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.textarea.TextComponentEditor;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.ui.JBUI;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.api.VimCommandLine;
import com.maddyhome.idea.vim.api.VimCommandLineCaret;
import com.maddyhome.idea.vim.helper.EngineStringHelper;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext;
import com.maddyhome.idea.vim.options.helpers.GuiCursorAttributes;
import com.maddyhome.idea.vim.options.helpers.GuiCursorMode;
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper;
import com.maddyhome.idea.vim.options.helpers.GuiCursorType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.Objects;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static java.lang.Math.ceil;
import static java.lang.Math.max;

/**
 * A custom text field for the Ex command line.
 * <p>
 * Note that because this is an instance of {@link JTextComponent}, anything that looks for an IntelliJ {@link Editor}
 * in current data context will get an instance of {@link TextComponentEditor}, which means that normal IntelliJ
 * shortcuts and actions will work with the Ex command line, even if they're not supposed to. E.g., CMD+V will paste on
 * Mac, but CTRL+V on Windows won't, because the Ex command line handles that shortcut. I don't see a way to fix this.
 * </p>
 */
public class ExTextField extends JTextField {
  private final ExEntryPanel myParentPanel;

  ExTextField(ExEntryPanel parentPanel) {
    myParentPanel = parentPanel;

    // We need to store this in a field, because we can't trust getCaret(), as it will return an instance of
    // ComposedTextCaret when working with dead keys or input methods
    caret = new CommandLineCaret();
    caret.setBlinkRate(getCaret().getBlinkRate());
    setCaret(caret);
    setNormalModeCaret();

    final Style defaultStyle = ((StyledDocument)getDocument()).getStyle(StyleContext.DEFAULT_STYLE);
    StyleConstants.setForeground(defaultStyle, getForeground());

    addCaretListener(e -> resetCaret());
  }

  void reset() {
    clearCurrentAction();
    setInsertMode();
  }

  void deactivate() {
    clearCurrentAction();
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
    setUI(new ExTextFieldUI(ExEditorKit.INSTANCE));
    invalidate();

    setBorder(null);
  }

  /**
   * Stores the current text as the last edited entry in the command line's history
   * <p>
   * This method is called whenever the text is changed by the user, either by typing, cut/paste or delete. It remembers
   * the current text as the last entry in the command line's history, so that we can scroll into the past, and then
   * return to the uncommitted text we were previously editing.
   * </p>
   */
  void saveLastEntry() {
    myParentPanel.setLastEntry(super.getText());
  }

  /**
   * Update the text in the text field without saving it as the current/last entry in the command line's history
   * <p>
   * This is used to update the text to another entry in the command line's history, without losing the current/last
   * entry.
   * </p>
   */
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

  public void insertText(int offset, String text) {
    try {
      // Note that ExDocument.insertString handles overwrite, but not replace mode!
      getDocument().insertString(offset, text, null);
    }
    catch (BadLocationException e) {
      logger.error(e);
    }
    saveLastEntry();
    setFontToJField(getText());
  }

  public void deleteText(int offset, int length) {
    try {
      getDocument().remove(offset, Math.min(length, getDocument().getLength() - offset));
    }
    catch (BadLocationException e) {
      logger.error(e);
    }
    saveLastEntry();
    setFontToJField(getText());
  }

  // VIM-570
  private void setFontToJField(String stringToDisplay) {
    setFont(UiHelper.selectEditorFont(myParentPanel.getIjEditor(), stringToDisplay));
  }

  @Override
  public void setFont(Font f) {
    super.setFont(f);
    final Document document = getDocument();
    if (document instanceof StyledDocument styledDocument) {
      final Style defaultStyle = styledDocument.getStyle(StyleContext.DEFAULT_STYLE);
      if (!Objects.equals(StyleConstants.getFontFamily(defaultStyle), getFont().getFamily())) {
        StyleConstants.setFontFamily(defaultStyle, getFont().getFamily());
      }
      if (!Objects.equals(StyleConstants.getFontSize(defaultStyle), getFont().getSize())) {
        StyleConstants.setFontSize(defaultStyle, getFont().getSize());
      }
    }
  }

  @Override
  public void setForeground(Color fg) {
    super.setForeground(fg);
    final Document document = getDocument();
    if (document instanceof StyledDocument styledDocument) {
      final Style defaultStyle = styledDocument.getStyle(StyleContext.DEFAULT_STYLE);
      StyleConstants.setForeground(defaultStyle, fg);
    }
  }

  public void setSpecialKeyForeground(Color fg) {
    final Document document = getDocument();
    if (document instanceof ExDocument exDocument) {
      exDocument.setSpecialKeyForeground(fg);
    }
  }

  /**
   * Finish handling the keystroke
   * <p>
   * When the key event is first received, the keystroke is sent to the key handler to handle commands or for mapping.
   * The potentially mapped keystroke is then returned to the text field to complete handling. Typically, the only
   * keystrokes passed are typed characters, along with pressed shortcuts that aren't recognised as commands (they won't
   * be recognised by the text field either; we don't register any commands).
   * </p>
   * @param stroke  The potentially mapped keystroke
   */
  public void handleKey(@NotNull KeyStroke stroke) {
    if (logger.isDebugEnabled()) logger.debug("stroke=" + stroke);

    // Typically, we would let the super class handle the keystroke. It would use any registered keybindings to convert
    // it to an action handler, or use the default handler (we don't actually have any keybindings). The default action
    // handler adds all non-control characters to the text field. We want to add all characters, so if we have an
    // actual character, just add it. Anything else, we'll pass to the super class like before (even though it's unclear
    // what it will do with the keystroke)
    if (stroke.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
      replaceSelection(String.valueOf(stroke.getKeyChar()));
    }
    else {
      //noinspection MagicConstant
      KeyEvent event = new KeyEvent(this, stroke.getKeyEventType(), (new Date()).getTime(), stroke.getModifiers(),
                                    stroke.getKeyCode(), stroke.getKeyChar());

      // Call super to avoid recursion!
      super.processKeyEvent(event);
    }

    saveLastEntry();
  }

  @Override
  protected void processKeyEvent(KeyEvent e) {
    if (logger.isDebugEnabled()) logger.debug("key=" + e);

    // The user has pressed or typed a key. The text field is first notified of a KEY_PRESSED event. If it's a simple
    // text character, it will be translated to a KEY_TYPED event with a keyChar, and we'll be notified again. We'll
    // forward this typed key to the key handler, which will potentially map it to another keystroke and return it to
    // the text field via ExTextField.handleKey.
    // If it's not a simple text character but a shortcut (i.e., a keypress with one or more modifiers), then it will be
    // handled in the same way as a shortcut activated in the main editor. That is, the IntelliJ action system sees the
    // KEY_PRESSED event first and dispatches it to IdeaVim's action handler, which pushes it through the key handler
    // and invokes the action. The event is handled before this component sees it! This method is not called for
    // registered action shortcuts. It is called for other shortcuts, and we pass those on to the super class.
    // (We should consider passing these to the key handler too, so that "insert literal" (<C-V>) will convert them to
    // typed keys and display them in the text field. As it happens, Vim has shortcuts for all control characters, and
    // it's unclear if/how we should represent other modifier-based shortcuts)
    // Note that Enter and Escape are registered as editor actions rather than action shortcuts. Therefore, we have to
    // handle them separately, as key presses, but not as typed characters.
    if (isAllowedPressedEvent(e) || isAllowedTypedEvent(e)) {
      var editor = myParentPanel.getEditor();
      var keyHandler = KeyHandler.getInstance();
      var keyStroke = KeyStroke.getKeyStrokeForEvent(e);
      keyHandler.handleKey(editor, keyStroke, new IjEditorExecutionContext(myParentPanel.getContext()),
                           keyHandler.getKeyHandlerState());
      e.consume();
    }
    else {
      super.processKeyEvent(e);
    }
  }

  private boolean isAllowedTypedEvent(KeyEvent event) {
    return event.getID() == KeyEvent.KEY_TYPED && !isKeyCharEnterOrEscape(event.getKeyChar());
  }

  private boolean isAllowedPressedEvent(KeyEvent event) {
    return event.getID() == KeyEvent.KEY_PRESSED && isKeyCodeEnterOrEscape(event.getKeyCode());
  }

  private boolean isKeyCharEnterOrEscape(char keyChar) {
    return keyChar == '\n' || keyChar == '\u001B';
  }

  private boolean isKeyCodeEnterOrEscape(int keyCode) {
    return keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE;
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

  public void clearCurrentAction() {
    VimCommandLine commandLine = injector.getCommandLine().getActiveCommandLine();
    if (commandLine != null) commandLine.clearPromptCharacter();
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
    if (getCaretPosition() == super.getText().length() ||
        currentActionPromptCharacterOffset == super.getText().length() - 1) {
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
      final Graphics2D g2d = (Graphics2D)g.create();
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
          final float outlineThickness = (float)PaintUtil.alignToInt(1.0, g2d);
          final double caretWidth = getCaretWidth(fm, r.getX(), 100, false);
          final Area area = new Area(new Rectangle2D.Double(r.getX(), r.getY(), caretWidth, r.getHeight()));
          area.subtract(new Area(new Rectangle2D.Double(r.getX() + outlineThickness, r.getY() + outlineThickness,
                                                        caretWidth - (2 * outlineThickness),
                                                        r.getHeight() - (2 * outlineThickness))));
          g2d.fill(area);
        }
        else {
          final double caretHeight = getCaretHeight(r.getHeight());
          final double caretWidth = getCaretWidth(fm, r.getX(), thickness, true);
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
        width = (int)ceil(getCaretWidth(fm, r.x, 100, false)) + 1;
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

    private double getCaretWidth(FontMetrics fm, double dotX, int widthPercentage, boolean coerceCharacterWidth) {
      // Caret width is based on the distance to the next character. This isn't necessarily the same as the character
      // width. E.g. when using float coordinates, the width of a grid is 8.4, while the character width is only 8. This
      // would give us a caret that is not wide enough.
      // We can also try to coerce to the width of the character, rather than using the View's width. This is so that
      // non-printable characters have the same sized caret as printable characters. The only time we use full width
      // with non-printable characters is when drawing the lost-focus caret (a box enclosing the character).
      double width;
      final Rectangle2D r = modelToView(getDot() + 1);
      if (r != null && !coerceCharacterWidth) {
        width = r.getX() - dotX;
      }
      else {
        char c = ' ';
        try {
          if (getDot() < getComponent().getDocument().getLength()) {
            char documentChar = getComponent().getText(getDot(), 1).charAt(0);
            if (EngineStringHelper.INSTANCE.isPrintableCharacter(documentChar)) {
              c = documentChar;
            }
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
    CommandLineCaret caret = (CommandLineCaret)getCaret();
    return String.format("%s %d", caret.mode, caret.thickness);
  }

  private final CommandLineCaret caret;
  int currentActionPromptCharacterOffset = -1;

  private static final Logger logger = Logger.getInstance(ExTextField.class.getName());
}

/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex;

import com.intellij.openapi.editor.textarea.TextComponentEditor;
import com.intellij.openapi.util.SystemInfo;
import com.maddyhome.idea.vim.api.VimCommandLine;
import com.maddyhome.idea.vim.helper.EngineStringHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Map;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;

/**
 * This document provides insert/overwrite mode
 * Note that PlainDocument will remove CRs from text for single line text fields
 */
public class ExDocument extends DefaultStyledDocument {
  public static final String SpecialKeyStyleName = "SpecialKey";
  public static final String NonPrintableElementName = "non-printable";

  private boolean overwrite = false;

  public ExDocument() {
    setDocumentFilter(new ExDocumentFilter());
    final Style specialStyle = addStyle(SpecialKeyStyleName, null);
    final Style nonPrintableStyle = addStyle(NonPrintableElementName, specialStyle);
    nonPrintableStyle.addAttribute(AbstractDocument.ElementNameAttribute, NonPrintableElementName);
  }

  /**
   * Set the foreground colour for special key characters
   * <p>
   * Maps to Vim's `SpecialKey` highlight
   * </p>
   */
  void setSpecialKeyForeground(@NotNull Color fg) {
    final Style style = getStyle(SpecialKeyStyleName);
    if (style != null) {
      StyleConstants.setForeground(style, fg);
    }
  }

  /**
   * Toggles the insert/overwrite state
   */
  void toggleInsertReplace() {
    VimCommandLine commandLine = injector.getCommandLine().getActiveCommandLine();
    if (commandLine != null) {
      ((ExEntryPanel)commandLine).isReplaceMode = !((ExEntryPanel)commandLine).isReplaceMode;
    }
    overwrite = !overwrite;
  }

  /**
   * Checks if this document is in insert or overwrite mode
   *
   * @return true if overwrite, false if insert mode
   */
  public boolean isOverwrite() {
    return overwrite;
  }

  @Override
  public void insertString(int offs, @NotNull String str, AttributeSet a) throws BadLocationException {
    addInputMethodAttributes(a);
    super.insertString(offs, str, a);
  }

  // Mac apps will show a highlight for text being composed as part of an input method or dead keys (e.g. <A-N> N will
  // combine a ~ and n to produce ñ on a UK/US keyboard, and `, ' or ~ will combine to add accents on US International
  // keyboards. Java only adds a highlight when the Input Method tells it to, so normal text fields don't get the
  // highlight for dead keys. However, it does make text composition a little easier and more obvious, especially when
  // working with incremental search, and the IntelliJ editor also shows it on Mac, so we'll add it to the ex entry
  // field. Note that Windows doesn't show dead key highlights at all, not even for the IntelliJ editor. I don't know
  // what Linux does
  private void addInputMethodAttributes(AttributeSet attributeSet) {
    if (!SystemInfo.isMac) {
      return;
    }

    // See if we have a composed text attribute, and if so, try to cast it to AttributedString
    final Object composedTextAttribute =
      attributeSet != null ? attributeSet.getAttribute(StyleConstants.ComposedTextAttribute) : null;
    if (composedTextAttribute instanceof AttributedString attributedString) {
      // Iterate over all the characters in the attributed string (might just be `~` or `^`, or might be a longer prefix
      // from an IME) and add an input method highlight to it if it's not already there
      final AttributedCharacterIterator iterator = attributedString.getIterator();
      while (iterator.current() != AttributedCharacterIterator.DONE) {
        final Map<AttributedCharacterIterator.Attribute, Object> currentCharAttributes = iterator.getAttributes();
        if (!currentCharAttributes.containsKey(TextAttribute.INPUT_METHOD_HIGHLIGHT) &&
            !currentCharAttributes.containsKey(TextAttribute.INPUT_METHOD_UNDERLINE)) {
          attributedString.addAttribute(TextAttribute.INPUT_METHOD_HIGHLIGHT,
                                        InputMethodHighlight.UNSELECTED_CONVERTED_TEXT_HIGHLIGHT, iterator.getIndex(),
                                        iterator.getIndex() + 1);
        }

        iterator.next();
      }
    }
  }

  /**
   * A document filter to apply special attributes to non-printable characters
   * <p>
   * Filters document changes to insert non-printable characters as separate changes with special attributes. This
   * allows us identifying document elements containing non-printable characters and create a different view for them,
   * rendering them appropriately.
   * </p>
   */
  private static class ExDocumentFilter extends DocumentFilter {
    /**
     * Invoked before inserting a region of text in the specified Document.
     * <p>
     * This is called by {@link AbstractDocument#insertString}, which has few callers in our usage. Most changes to the
     * text happen by replacing either the entire text or the selection, via {@link AbstractDocument#replace} (and
     * therefore also @{link DocumentFilter#replace}). Some things will still call {@link AbstractDocument#insertString}
     * such as IntelliJ's own paste action, which operates on the Ex text field component because it gets a
     * {@link TextComponentEditor} instance from the current data context.
     * </p>
     */
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
      throws BadLocationException {
      final int originalOffset = offset;

      string = filterNewLines(fb.getDocument(), string);

      int start = 0;
      int pos = 0;
      while (pos < string.length()) {
        //noinspection DuplicatedCode
        final boolean isPrintable = isPrintableCharacter(string.charAt(pos));
        while (pos < string.length() && isPrintableCharacter(string.charAt(pos)) == isPrintable) {
          pos++;
        }

        final AttributeSet a = isPrintable ? attr : getNonPrintableAttributes(fb.getDocument(), attr);
        final String s = string.substring(start, pos);
        fb.insertString(offset, s, a);
        offset += s.length();

        start = pos;
      }

      if (fb.getDocument() instanceof ExDocument exDocument && exDocument.isOverwrite()) {
        int rest = originalOffset + string.length();
        if (rest < exDocument.getLength()) {
          int len = Math.min(string.length(), exDocument.getLength() - rest);
          fb.remove(rest, len);
        }
      }
    }

    /**
     * Invoked before replacing a region of text in the specified Document.
     * <p>
     * This method is called for most changes to the document. When handing a {@link KeyEvent#KEY_TYPED} event, the
     * document is modified with a call to {@link JTextComponent#replaceSelection}, which calls
     * {@link AbstractDocument#replace}, passing the last offset and a length of 0. If client code calls
     * {@link JTextComponent#setText}, this will call {@link AbstractDocument#replace} with an offset of 0 and the full
     * text length.
     * </p>
     */
    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
      throws BadLocationException {

      if (text.isEmpty()) {
        fb.replace(offset, length, text, attrs);
        return;
      }

      text = filterNewLines(fb.getDocument(), text);

      if (fb.getDocument() instanceof ExDocument exDocument && exDocument.isOverwrite()) {
        if (offset + text.length() > exDocument.getLength()) {
          length = exDocument.getLength() - offset;
        }
        else {
          length = Math.max(length, text.length());
        }
      }

      int start = 0;
      int pos = 0;
      while (pos < text.length()) {
        //noinspection DuplicatedCode
        final boolean isPrintable = isPrintableCharacter(text.charAt(pos));
        while (pos < text.length() && isPrintableCharacter(text.charAt(pos)) == isPrintable) {
          pos++;
        }

        // Note that fb.replace will remove the existing text and then add the new text. If we're replacing the whole
        // text (e.g., ExTextField.setText) then this can reset the scroll position. Note that ExTextField has methods
        // to set, insert and delete text to avoid this situation.
        final AttributeSet a = isPrintable ? attrs : getNonPrintableAttributes(fb.getDocument(), attrs);
        final String s = text.substring(start, pos);
        if (start == 0) {
          fb.replace(offset, length, s, a);
        }
        else {
          fb.insertString(offset, s, a);
        }
        offset += s.length();

        start = pos;
      }
    }

    private String filterNewLines(Document document, String string) {
      // A text field shouldn't have new lines
      Object filterNewlines = document.getProperty("filterNewlines");
      if ((filterNewlines instanceof Boolean) && filterNewlines.equals(Boolean.TRUE)) {
        return string.replaceAll("\n", " ");
      }
      return string;
    }

    private static boolean isPrintableCharacter(char c) {
      return EngineStringHelper.INSTANCE.isPrintableCharacter(c);
    }

    private static @NotNull AttributeSet getNonPrintableAttributes(Document document, AttributeSet attrs) {
      if (document instanceof ExDocument exDocument) {
        return exDocument.getStyle(ExDocument.NonPrintableElementName);
      }
      else {
        final SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        if (attrs != null) {
          attributeSet.addAttributes(attrs);
        }
        attributeSet.addAttribute(AbstractDocument.ElementNameAttribute, ExDocument.NonPrintableElementName);
        return attributeSet;
      }
    }
  }
}

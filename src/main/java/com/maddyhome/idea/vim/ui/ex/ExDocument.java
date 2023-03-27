/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.*;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Map;

/**
 * This document provides insert/overwrite mode
 * Note that PlainDocument will remove CRs from text for single line text fields
 */
public class ExDocument extends PlainDocument {
  private boolean overwrite = false;

  /**
   * Toggles the insert/overwrite state
   */
  void toggleInsertReplace() {
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

  /**
   * Inserts some content into the document.
   * Inserting content causes a write lock to be held while the
   * actual changes are taking place, followed by notification
   * to the observers on the thread that grabbed the write lock.
   * <p/>
   * This method is thread safe, although most Swing methods
   * are not. Please see
   * <A HREF="https://java.sun.com/products/jfc/swingdoc-archive/threads.html">Threads
   * and Swing</A> for more information.
   *
   * @param offs the starting offset >= 0
   * @param str  the string to insert; does nothing with null/empty strings
   * @param a    the attributes for the inserted content
   * @throws BadLocationException the given insert position is not a valid
   *                              position within the document
   * @see Document#insertString
   */
  @Override
  public void insertString(int offs, @NotNull String str, AttributeSet a) throws BadLocationException {

    addInputMethodAttributes(a);

    super.insertString(offs, str, a);
    int newOffs = offs + str.length();
    if (overwrite && newOffs < getLength()) {
      int len = Math.min(str.length(), getLength() - newOffs);
      super.remove(newOffs, len);
    }
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

    final Object attribute =
      attributeSet != null ? attributeSet.getAttribute(StyleConstants.ComposedTextAttribute) : null;
    if (attribute instanceof AttributedString as) {
      final Map<AttributedCharacterIterator.Attribute, Object> attributes = as.getIterator().getAttributes();
      if (!attributes.containsKey(TextAttribute.INPUT_METHOD_HIGHLIGHT) &&
        !attributes.containsKey(TextAttribute.INPUT_METHOD_UNDERLINE)) {
        as.addAttribute(TextAttribute.INPUT_METHOD_HIGHLIGHT, InputMethodHighlight.UNSELECTED_CONVERTED_TEXT_HIGHLIGHT);
      }
    }
  }
}

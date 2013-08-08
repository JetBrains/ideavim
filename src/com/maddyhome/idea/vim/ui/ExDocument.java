/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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

import org.jetbrains.annotations.NotNull;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * This document provides insert/overwrite mode
 */
public class ExDocument extends PlainDocument {
  /**
   * Toggles the insert/overwrite state
   */
  public void toggleInsertReplace() {
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
   * <A HREF="http://java.sun.com/products/jfc/swingdoc-archive/threads.html">Threads
   * and Swing</A> for more information.
   *
   * @param offs the starting offset >= 0
   * @param str  the string to insert; does nothing with null/empty strings
   * @param a    the attributes for the inserted content
   * @throws BadLocationException the given insert position is not a valid
   *                              position within the document
   * @see Document#insertString
   */
  public void insertString(int offs, @NotNull String str, AttributeSet a) throws BadLocationException {
    super.insertString(offs, str, a);
    int newOffs = offs + str.length();
    if (overwrite && newOffs < getLength()) {
      int len = Math.min(str.length(), getLength() - newOffs);
      super.remove(newOffs, len);
    }
  }

  protected boolean overwrite = false;
}

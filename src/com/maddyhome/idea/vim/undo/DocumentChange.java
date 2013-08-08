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

package com.maddyhome.idea.vim.undo;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class DocumentChange {
  public DocumentChange(int offset, String oldText, String newText) {
    this.offset = offset;
    this.oldText = oldText;
    this.newText = newText;
  }

  public int getOffset() {
    return offset;
  }

  public String getOldText() {
    return oldText;
  }

  public String getNewText() {
    return newText;
  }

  public void redo(@NotNull Editor editor, DataContext context) {
    if (oldText.length() > 0) {
      if (newText.length() > 0) {
        editor.getDocument().replaceString(offset, offset + oldText.length(), newText);
      }
      else {
        editor.getDocument().deleteString(offset, offset + oldText.length());
      }
    }
    else {
      if (newText.length() > 0) {
        editor.getDocument().insertString(offset, newText);
      }
      else {
        // Both empty - no-op
      }
    }
  }

  public void undo(@NotNull Editor editor, DataContext context) {
    if (logger.isDebugEnabled()) logger.debug("undo command = " + this);
    if (oldText.length() > 0) {
      if (newText.length() > 0) {
        editor.getDocument().replaceString(offset, offset + newText.length(), oldText);
      }
      else {
        editor.getDocument().insertString(offset, oldText);
      }
    }
    else {
      if (newText.length() > 0) {
        editor.getDocument().deleteString(offset, offset + newText.length());
      }
      else {
        // Both empty - no-op
      }
    }
  }

  @NotNull
  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("DocumentChange[");
    res.append("offset=").append(offset);
    res.append(", oldText=\"").append(oldText);
    res.append("\", newText=\"").append(newText);
    res.append("\"]");

    return res.toString();
  }

  private int offset;
  private String oldText;
  private String newText;

  private static Logger logger = Logger.getInstance(DocumentChange.class.getName());
}

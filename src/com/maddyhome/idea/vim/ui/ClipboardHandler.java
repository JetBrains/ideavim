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

import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * This is a utility class for working with the system clipboard
 */
public class ClipboardHandler {
  /**
   * Returns the string currently on the system clipboard.
   *
   * @return The clipboard string or null if data isn't plain text
   */
  @Nullable
  public static String getClipboardText() {
    String res = null;
    try {
      Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable trans = board.getContents(null);
      Object data = trans.getTransferData(DataFlavor.stringFlavor);

      if (data != null) {
        res = data.toString();
      }
    }
    catch (HeadlessException e) {
      // ignore
    }
    catch (UnsupportedFlavorException e) {
      // ignore
    }
    catch (IOException e) {
      // ignore
    }

    return res;
  }

  /**
   * Puts the supplied text into the system clipboard
   *
   * @param text The text to add to the clipboard
   */
  public static void setClipboardText(String text) {
    try {
      Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
      StringSelection data = new StringSelection(text);
      board.setContents(data, null);
    }
    catch (HeadlessException e) {
      // ignore
    }
  }
}

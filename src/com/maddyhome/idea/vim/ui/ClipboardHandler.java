/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.RawText;
import com.maddyhome.idea.vim.helper.TestClipboardModel;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class for working with the system clipboard
 */
public class ClipboardHandler {
  /**
   * Returns the string currently on the system clipboard.
   *
   * @return The clipboard string or null if data isn't plain text
   */
  public static @NotNull Pair<String, List<TextBlockTransferableData>> getClipboardTextAndTransferableData() {
    String res = null;
    List<TextBlockTransferableData> transferableData = new ArrayList<>();
    try {
      Transferable trans = getContents();
      Object data = trans.getTransferData(DataFlavor.stringFlavor);

      res = data.toString();
      transferableData = collectTransferableData(trans);
    }
    catch (HeadlessException | UnsupportedFlavorException | IOException ignored) {
    }

    return new Pair<>(res, transferableData);
  }

  private static List<TextBlockTransferableData> collectTransferableData(Transferable transferable) {
    List<TextBlockTransferableData> allValues = new ArrayList<>();
    for (CopyPastePostProcessor<? extends TextBlockTransferableData> processor : CopyPastePostProcessor.EP_NAME
      .getExtensionList()) {
      List<? extends TextBlockTransferableData> data = processor.extractTransferableData(transferable);
      if (!data.isEmpty()) {
        allValues.addAll(data);
      }
    }
    return allValues;
  }

  /**
   * Puts the supplied text into the system clipboard
   *
   * @param text The text to add to the clipboard
   */
  public static void setClipboardText(String text, List<TextBlockTransferableData> transferableData, String rawText) {
    try {
      final String s = TextBlockTransferable.convertLineSeparators(text, "\n", transferableData);
      TextBlockTransferable content = new TextBlockTransferable(s, transferableData, new RawText(rawText));
      setContents(content);
    }
    catch (HeadlessException ignored) {
    }
  }

  private static @NotNull Transferable getContents() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return TestClipboardModel.INSTANCE.getContents();
    }

    Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
    return board.getContents(null);
  }

  private static void setContents(@NotNull Transferable contents) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      TestClipboardModel.INSTANCE.setContents(contents);
    }
    else {
      Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
      board.setContents(contents, null);
    }
  }
}

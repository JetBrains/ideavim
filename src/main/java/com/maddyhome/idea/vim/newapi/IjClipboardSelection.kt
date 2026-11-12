/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.ide.CopyPasteManagerEx
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

/**
 * The CLIPBOARD selection, the regular clipboard of every system, reached through the IDE clipboard
 * (`CopyPasteManagerEx`), which keeps the IDE metadata of a copy and the clipboard history.
 *
 * The parameters replace the clipboards in tests.
 */
internal class IjClipboardSelection(
  private val ideClipboardContents: () -> Transferable? = { CopyPasteManagerEx.getInstanceEx().contents },
  private val x11ClipboardText: () -> String? = ::readX11ClipboardText,
) {
  /** The text in the clipboard with its IDE metadata, or null if the clipboard has no text. */
  fun read(): IjVimCopiedText? = getContents()?.let(IjCopiedTextCollector::fromTransferable)

  /**
   * The IDE clipboard comes first, as it keeps the IDE metadata of a copy (e.g. the references `ideaput` uses for
   * imports). On X11 it can come back empty for text owned by another application, though: the platform first asks the
   * owner for the clipboard formats with a 300 ms timeout and reports an empty clipboard when the answer is late. The
   * system clipboard is read directly then, with the regular AWT timeout, as the `*` register already does.
   */
  private fun getContents(): Transferable? {
    val ideContents = ideClipboardContents()
    if (ideContents != null && ideContents.isDataFlavorSupported(DataFlavor.stringFlavor)) return ideContents
    val text = x11ClipboardText() ?: return ideContents
    logger.debug("The IDE clipboard has no text, the text was read from the X11 clipboard")
    return StringSelection(text)
  }

  companion object {
    private val logger = vimLogger<IjClipboardSelection>()

    /**
     * Puts [text] into the IDE clipboard, which adds it to the clipboard history. Returns the content that was put
     * there, for code that removes it from the history later, or null on a headless JVM.
     */
    fun write(text: String, rawText: String, transferableData: List<Any>): TextBlockTransferable? {
      return try {
        buildIjTextTransferable(text, rawText, transferableData).also {
          CopyPasteManagerEx.getInstanceEx().setContents(it)
        }
      } catch (_: HeadlessException) {
        null
      }
    }
  }
}

/**
 * Reads the text of the X11 CLIPBOARD selection through AWT, bypassing `ClipboardSynchronizer`. Returns null on any
 * other windowing system, where the IDE clipboard doesn't lose the content of other applications.
 */
private fun readX11ClipboardText(): String? {
  return try {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    // The same check the platform uses to apply its short X11 timeout
    if (clipboard.javaClass.name != "sun.awt.X11.XClipboard") return null
    clipboard.getData(DataFlavor.stringFlavor) as? String
  } catch (_: HeadlessException) {
    null
  } catch (_: IllegalStateException) {
    null
  } catch (_: UnsupportedFlavorException) {
    null
  } catch (_: IOException) {
    null
  }
}

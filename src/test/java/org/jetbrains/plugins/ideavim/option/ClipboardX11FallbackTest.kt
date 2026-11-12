/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.openapi.editor.CaretStateTransferableData
import com.intellij.openapi.editor.RawText
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.newapi.IjClipboardSelection
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * On X11 the IDE clipboard reports an empty clipboard when the owner of the clipboard (another application) answers
 * the request for the clipboard formats later than in 300 ms. The `+` register is read from the X11 clipboard then.
 */
class ClipboardX11FallbackTest : VimTestCase() {
  private var x11Reads = 0

  private fun readClipboard(ideContents: Transferable?, x11Text: String?): VimCopiedText? {
    configureByText("${c}Lorem ipsum")
    val clipboard = IjClipboardSelection(
      ideClipboardContents = { ideContents },
      x11ClipboardText = {
        x11Reads++
        x11Text
      },
    )
    return clipboard.read()
  }

  @Test
  fun `test clipboard text of another application is read from X11 when the IDE clipboard is empty`() {
    val content = readClipboard(ideContents = null, x11Text = "EXTERNAL")
    assertEquals("EXTERNAL", content?.text)
  }

  @Test
  fun `test clipboard text is read from X11 when the IDE clipboard has no text`() {
    val content = readClipboard(ideContents = NoTextTransferable, x11Text = "EXTERNAL")
    assertEquals("EXTERNAL", content?.text)
  }

  @Test
  fun `test IDE clipboard content is preferred over X11`() {
    // The IDE transferable is what keeps the metadata of an IDE copy, e.g. for the imports of ideaput
    val caretState = CaretStateTransferableData(intArrayOf(0), intArrayOf(3))
    val ideContents = TextBlockTransferable("IDE", listOf(caretState), RawText("IDE"))

    val content = readClipboard(ideContents, x11Text = "EXTERNAL")

    assertEquals("IDE", content?.text)
    assertEquals(0, x11Reads, "X11 clipboard must not be read when the IDE clipboard has text")
  }

  @Test
  fun `test clipboard is empty when neither the IDE nor X11 has text`() {
    // Also what happens outside X11, where the X11 clipboard reader returns null
    val content = readClipboard(ideContents = null, x11Text = null)
    assertNull(content)
    assertEquals(1, x11Reads)
  }

  @Test
  fun `test empty IDE clipboard without X11 text stays empty`() {
    val content = readClipboard(ideContents = NoTextTransferable, x11Text = null)
    assertNull(content)
    assertEquals(1, x11Reads)
  }

  private object NoTextTransferable : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.javaFileListFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.javaFileListFlavor
    override fun getTransferData(flavor: DataFlavor): Any {
      if (flavor != DataFlavor.javaFileListFlavor) throw UnsupportedFlavorException(flavor)
      return emptyList<Any>()
    }
  }
}

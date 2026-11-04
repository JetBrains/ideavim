/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * With `clipboard=ideaput` a put goes through the IDE paste, which reads the text from the clipboard. The register
 * is put into the clipboard only for that paste, so the system clipboard has to be the same afterwards.
 */
class PutClipboardSplitTest : IdeaVimStarterTestBase() {

  override fun ideaVimRcContent(): String = "set clipboard=ideaput\n"

  @Test
  fun `put with ideaput keeps the system clipboard`() {
    openFile(createFile("src/PutClipboard1.txt", "first line\nsecond line\n"))

    typeVim("yy")
    pause()
    setSystemClipboard("EXTERNAL")
    pause()

    typeVim("p")
    assertEditorContains("first line\nfirst line\nsecond line", "The yanked line should be put")
    pause(2000)

    assertEquals("EXTERNAL", systemClipboardText()) { "The put should not change the system clipboard" }
  }
}

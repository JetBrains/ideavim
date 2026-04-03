/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Test

class JumpNavigationSplitTest : IdeaVimStarterTestBase() {

  private fun longFile(name: String): String {
    val lines = (1..50).joinToString("\n") { "Line $it of content" }
    return createFile("src/$name.txt", lines + "\n")
  }

  @Test
  fun `G motion creates jump and Ctrl-O navigates back`() {
    openFile(longFile("Jump1"))

    typeVim("G")
    pause(500)
    assertCaretAfter(40, "G should go to end of file")

    ctrlO()
    pause(500)
    assertCaretBefore(10, "Ctrl-O should jump back to start")
  }

  @Test
  fun `search creates jump and Ctrl-O navigates back`() {
    openFile(longFile("Jump2"))

    typeVim("gg")
    pause(500)

    typeVim("/Line 30\n")
    pause()
    assertCaretAfter(20, "Search should jump past line 20")

    esc()
    ctrlO()
    pause()
    assertCaretBefore(10, "Ctrl-O should jump back near start after search")
  }

  @Test
  fun `IDE Back navigation records jump so apostrophe mark navigates back`() {
    openFile(longFile("Jump3"))

    typeVim("G")
    pause(300)
    assertCaretAfter(40, "G should move to end of file")

    ideaGoBack()
    pause(500)
    assertCaretBefore(10, "IDE Back should return to start of file")

    typeVim("''")
    pause(300)
    assertCaretAfter(40, "'' should return to position before IDE Back (end of file)")
  }
}

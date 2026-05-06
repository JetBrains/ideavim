/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Test

/**
 * Split-mode coverage for VIM-519 (`g;` / `g,`).
 *
 * In split mode the change list is fed by the backend's `RecentPlacesListener`
 * over `CHANGE_LIST_REMOTE_TOPIC`. These tests verify the full pipeline:
 * edit on backend → topic broadcast → frontend `ChangeListService` → `g;` reads it.
 *
 * Index logic and error paths are covered by `MotionGotoChangeActionTest` in the
 * monolith suite; here we only check that the RPC topic delivers events at all.
 */
class ChangeListSplitTest : IdeaVimStarterTestBase() {

  private fun longFile(name: String): String {
    val lines = (1..20).joinToString("\n") { "Line $it of content" }
    return createFile("src/$name.txt", lines + "\n")
  }

  @Test
  fun `g_semicolon returns to last edit after moving away`() {
    openFile(longFile("ChangeList1"))

    typeVim("5G")
    pause()
    typeVim("rX")
    pause(1000)

    typeVim("G")
    pause()
    assertCaretAfter(15, "G should go past line 15")

    typeVim("g;")
    pause(1000)
    assertCaretAtLine(5, "g; should bring cursor back to the edited line via the RPC topic")
  }

  @Test
  fun `g_semicolon walks back through multiple edits`() {
    openFile(longFile("ChangeList2"))

    typeVim("3G")
    pause()
    typeVim("rA")
    pause(800)

    typeVim("8G")
    pause()
    typeVim("rB")
    pause(800)

    typeVim("13G")
    pause()
    typeVim("rC")
    pause(800)

    typeVim("G")
    pause()

    typeVim("g;")
    pause(800)
    assertCaretAtLine(13, "first g; lands on the newest edit")

    typeVim("g;")
    pause(800)
    assertCaretAtLine(8, "second g; walks back one entry")

    typeVim("g;")
    pause(800)
    assertCaretAtLine(3, "third g; walks back to the oldest entry")
  }
}

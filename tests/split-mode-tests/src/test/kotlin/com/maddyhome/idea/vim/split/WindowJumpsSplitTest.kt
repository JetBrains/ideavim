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
 * Smoke tests for the `'ideawindowjumps'` option in split mode
 *
 * The jump list is scoped to a window by composing the project id with an id for the containing `EditorWindow`, resolved
 * through `FileEditorManagerEx`. That only exists on the frontend, and the service handing out the ids is registered in
 * `IdeaVIM.ideavim-frontend.xml`, so these tests check that per window jump lists survive the frontend/backend split -
 * every other test for this option runs in a monolith.
 *
 * The two splits show *different* files on purpose: `codeEditor()` requires exactly one editor component, so a split of
 * the same buffer cannot be addressed at all from the driver. See the helpers in [IdeaVimStarterTestBase].
 */
class WindowJumpsSplitTest : IdeaVimStarterTestBase() {

  override fun ideaVimRcContent(): String = "set ideawindowjumps\n"

  private fun longFile(name: String): String {
    val lines = (1..50).joinToString("\n") { "Line $it of content" }
    return createFile("src/$name.txt", lines + "\n")
  }

  @Test
  fun `jump list works when scoped to a window`() {
    openFile(longFile("WindowJumps1"))

    typeVim("G")
    pause(500)
    assertCaretAfter(40, "G should go to end of file")

    ctrlO()
    pause(500)
    // Reaching the jump means the window's scope was resolved on the frontend and the jump was recorded under it
    assertCaretBefore(10, "Ctrl-O should jump back to start with 'ideawindowjumps' set")
  }

  @Test
  fun `jumps made in one split do not appear in the other`() {
    val first = longFile("WindowJumpsA")
    val second = longFile("WindowJumpsB")
    openFile(first)

    // Recorded in the original window's list, while it is still the only window
    typeVim("G")
    pause(500)
    assertCaretAfter(40, "G should go to the end of the first file")

    // Split, then give the new window a file of its own. `:vsplit {path}` would be shorter, but the driver can only
    // address an editor by the file it shows, so two splits of the same buffer are indistinguishable
    exCommand("vsplit")
    pause(1000)
    openFileInCurrentWindow(second, "WindowJumpsB.txt")

    // Recorded in the new window's list
    typeVimInFile("WindowJumpsB.txt", "G")
    pause(500)
    assertCaretInFileAfter("WindowJumpsB.txt", 40, "G should go to the end of the second file")

    // The original window walks its own history, which never saw the second file. With one shared list its newest entry
    // would be in the other file, and this caret would stay where it is
    ctrlOInFile("WindowJumpsA.txt")
    pause(1000)
    assertCaretInFileBefore("WindowJumpsA.txt", 10, "Ctrl-O should walk this window's own jump list")
    assertCaretInFileAfter("WindowJumpsB.txt", 40, "The other window's caret should not have moved")
  }
}

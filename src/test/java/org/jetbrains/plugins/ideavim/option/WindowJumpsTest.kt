/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.group.JumpRemoteTopicListener
import com.maddyhome.idea.vim.group.jump.JumpInfo
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimSplitWindowTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the scope of the jump list - one list shared by the whole project, or one list per window
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "These tests need more than one window showing the same buffer. Neovim testing drives a single editor, " +
    "so there is nothing to compare split specific behaviour against.",
)
class WindowJumpsTest : VimSplitWindowTestCase() {
  private val loremText = """I found ${c}it in a legendary land
    |all rocks and lavender and tufted grass,
    |where it was settled on some sodden sand
    |hard by the torrent of a mountain pass.
    |
    |The features it combines mark it as new
    |to science: shape and shade -- the special tinge,
    |akin to moonlight, tempering its blue,
    |the dingy underside, the checquered fringe.
  """.trimMargin()

  private fun configureMainWindow(): Editor {
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
      editor = configureByText(loremText)
    }
    return editor!!
  }

  @Test
  fun `test ideawindowjumps is off by default`() {
    configureMainWindow()

    assertCommandOutput("set ideawindowjumps?", "noideawindowjumps")
  }

  @Test
  fun `test ideawindowjumps can be enabled`() {
    configureMainWindow()

    enterCommand("set ideawindowjumps")

    assertCommandOutput("set ideawindowjumps?", "  ideawindowjumps")
  }

  @Test
  fun `test ideawindowjumps can be disabled again`() {
    configureMainWindow()

    enterCommand("set ideawindowjumps")
    enterCommand("set noideawindowjumps")

    assertCommandOutput("set ideawindowjumps?", "noideawindowjumps")
  }

  @Test
  fun `test ideawindowjumps is a global option`() {
    configureMainWindow()

    enterCommand("setlocal ideawindowjumps")

    assertCommandOutput("setglobal ideawindowjumps?", "  ideawindowjumps")
  }

  @Test
  fun `test jump list is shared between splits`() {
    val mainWindow = configureMainWindow()
    enterSearch("sodden")
    enterSearch("shape")

    val splitWindow = openSplitWindow(mainWindow)
    selectWindow(splitWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 I found it in a legendary land
        |   1     3   29 where it was settled on some sodden sand
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test jumps recorded in a split are visible in the other split`() {
    val mainWindow = configureMainWindow()
    val splitWindow = openSplitWindow(mainWindow)

    // Jump to a line that the other window's caret has never visited, so we know the entry comes from this split
    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")

    selectWindow(mainWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 I found it in a legendary land
        |   1     9    0 the dingy underside, the checquered fringe.
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test jump list survives switching to another buffer in the same split`() {
    val mainWindow = configureMainWindow()
    enterSearch("sodden")
    enterSearch("shape")

    val mainWindowPath = mainWindow.virtualFile!!.path
    val otherBufferWindow = openNewBufferWindow("bbb.txt")
    selectWindow(otherBufferWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 $mainWindowPath
        |   1     3   29 $mainWindowPath
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test control-O in a split walks the history recorded in the other split`() {
    val mainWindow = configureMainWindow()
    enterSearch("sodden")
    enterSearch("shape")

    val splitWindow = openSplitWindow(mainWindow)
    selectWindow(splitWindow)
    typeText("<C-O>")

    // The newest entry of the shared jump list - the position the other split was at before searching for "shape"
    assertState(
      """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some ${c}sodden sand
        |hard by the torrent of a mountain pass.
        |
        |The features it combines mark it as new
        |to science: shape and shade -- the special tinge,
        |akin to moonlight, tempering its blue,
        |the dingy underside, the checquered fringe.
      """.trimMargin(),
    )
  }


  @Test
  @Disabled
  fun `test jumps recorded in one split do not appear in the other when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    disableIdeNavigationMirroring()
    val splitWindow = openSplitWindow(mainWindow)

    selectWindow(mainWindow)
    enterSearch("sodden")
    enterSearch("shape")

    selectWindow(splitWindow)

    assertCommandOutput("jumps", " jump line  col file/text\n>")
  }

  @Test
  fun `test jumps recorded in a split stay in that split when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    disableIdeNavigationMirroring()
    val splitWindow = openSplitWindow(mainWindow)

    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")

    selectWindow(mainWindow)

    assertCommandOutput("jumps", " jump line  col file/text\n>")
  }

  @Test
  fun `test control-O does not use the other split's history when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    disableIdeNavigationMirroring()
    val splitWindow = openSplitWindow(mainWindow)

    selectWindow(mainWindow)
    enterSearch("sodden")
    enterSearch("shape")

    selectWindow(splitWindow)
    typeText("<C-O>")

    // This split has no history of its own, so there is nowhere to jump back to and the caret stays put
    assertState(
      """I found ${c}it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
        |
        |The features it combines mark it as new
        |to science: shape and shade -- the special tinge,
        |akin to moonlight, tempering its blue,
        |the dingy underside, the checquered fringe.
      """.trimMargin(),
    )
  }

  @Test
  fun `test clearjumps only clears the current window when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    val splitWindow = openSplitWindow(mainWindow)

    selectWindow(mainWindow)
    enterSearch("sodden")
    enterSearch("shape")

    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")
    enterCommand("clearjumps")

    selectWindow(mainWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 I found it in a legendary land
        |   1     3   29 where it was settled on some sodden sand
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test jump list survives switching to another buffer in the same split when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    enterSearch("sodden")
    enterSearch("shape")

    val mainWindowPath = mainWindow.virtualFile!!.path
    val otherBufferWindow = openNewBufferWindow("bbb.txt")
    selectWindow(otherBufferWindow)

    // A Vim window keeps its jump list when the buffer it shows changes. Note that a new buffer in the same split is a
    // new IntelliJ editor, so this only holds while the list is scoped to the editor *window*
    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 $mainWindowPath
        |   1     3   29 $mainWindowPath
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test control-I returns to the position control-O jumped from when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    val splitWindow = openSplitWindow(mainWindow)

    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")
    typeText("<C-O>")
    typeText("<C-I>")

    assertState(
      """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the ${c}torrent of a mountain pass.
        |
        |The features it combines mark it as new
        |to science: shape and shade -- the special tinge,
        |akin to moonlight, tempering its blue,
        |the dingy underside, the checquered fringe.
      """.trimMargin(),
    )
  }

  @Test
  fun `test control-I does not use the other split's history when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    disableIdeNavigationMirroring()
    val splitWindow = openSplitWindow(mainWindow)

    selectWindow(mainWindow)
    enterSearch("sodden")
    enterSearch("shape")
    typeText("<C-O>")

    // The other split has somewhere to jump forward to, this one has no history at all
    selectWindow(splitWindow)
    typeText("<C-I>")

    assertState(
      """I found ${c}it in a legendary land
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
        |
        |The features it combines mark it as new
        |to science: shape and shade -- the special tinge,
        |akin to moonlight, tempering its blue,
        |the dingy underside, the checquered fringe.
      """.trimMargin(),
    )
  }

  @Test
  fun `test split copies the jump list from the opening window when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    enterSearch("sodden")
    enterSearch("shape")

    val splitWindow = openSplitWindow(mainWindow)
    selectWindow(splitWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 I found it in a legendary land
        |   1     3   29 where it was settled on some sodden sand
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test split copies the current jump spot too when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    enterSearch("sodden")
    enterSearch("shape")
    typeText("<C-O>")

    val expected = """ jump line  col file/text
      |   1     1    8 I found it in a legendary land
      |>  0     3   29 where it was settled on some sodden sand
      |   1     7   12 to science: shape and shade -- the special tinge,
    """.trimMargin()

    // The opening window has walked back one entry, so its spot is no longer at the end of the list
    assertCommandOutput("jumps", expected)

    val splitWindow = openSplitWindow(mainWindow)
    selectWindow(splitWindow)

    assertCommandOutput("jumps", expected)
  }

  @Test
  fun `test jumps made in the new split do not leak back to the opening window when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    enterSearch("sodden")
    enterSearch("shape")

    val splitWindow = openSplitWindow(mainWindow)
    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")

    selectWindow(mainWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 I found it in a legendary land
        |   1     3   29 where it was settled on some sodden sand
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test reopened split starts from a fresh copy when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    enterSearch("sodden")
    enterSearch("shape")

    val splitWindow = openSplitWindow(mainWindow)
    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")
    closeWindow(splitWindow)

    selectWindow(mainWindow)
    val reopenedSplitWindow = openSplitWindow(mainWindow)
    selectWindow(reopenedSplitWindow)

    // A copy of the opening window's list, with nothing left over from the split that was closed
    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 I found it in a legendary land
        |   1     3   29 where it was settled on some sodden sand
        |>
      """.trimMargin(),
    )
  }


  @Test
  fun `test enabling ideawindowjumps seeds every open window from the shared list`() {
    val mainWindow = configureMainWindow()
    enterSearch("sodden")
    enterSearch("shape")
    val splitWindow = openSplitWindow(mainWindow)

    enterCommand("set ideawindowjumps")

    val expected = """ jump line  col file/text
      |   2     1    8 I found it in a legendary land
      |   1     3   29 where it was settled on some sodden sand
      |>
    """.trimMargin()

    selectWindow(mainWindow)
    assertCommandOutput("jumps", expected)

    selectWindow(splitWindow)
    assertCommandOutput("jumps", expected)
  }

  @Test
  fun `test windows seeded when enabling ideawindowjumps get independent copies`() {
    val mainWindow = configureMainWindow()
    enterSearch("sodden")
    enterSearch("shape")
    val splitWindow = openSplitWindow(mainWindow)

    enterCommand("set ideawindowjumps")

    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")

    selectWindow(mainWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 I found it in a legendary land
        |   1     3   29 where it was settled on some sodden sand
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test disabling ideawindowjumps keeps the focused window's list`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    val splitWindow = openSplitWindow(mainWindow)

    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")

    selectWindow(mainWindow)
    enterSearch("sodden")

    // The split is the focused window when the option goes off, so its list is the one that survives
    selectWindow(splitWindow)
    enterCommand("set noideawindowjumps")

    selectWindow(mainWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 I found it in a legendary land
        |   1     9    0 the dingy underside, the checquered fringe.
        |>
      """.trimMargin(),
    )
  }


  @Test
  fun `test IDE navigation is recorded in the window showing the file when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    val splitWindow = openSplitWindow(mainWindow)

    selectWindow(splitWindow)
    recordPlatformJump(splitWindow, line = 5, col = 3)

    // The lists cannot be asserted exactly: the platform mirrors the file being opened as a jump of its own, and when
    // that arrives is not something the test controls. What matters is which window the navigation was recorded in
    val recordedJump = "6    3 The features it combines mark it as new"
    assertTrue(commandOutput("jumps").contains(recordedJump), "The navigation should be recorded in this window")

    selectWindow(mainWindow)
    assertFalse(commandOutput("jumps").contains(recordedJump), "The other window should not see it")
  }

  /** Feeds an IDE navigation event to the listener behind the `ideaunifyjumps` sync */
  private fun recordPlatformJump(editor: Editor, line: Int, col: Int) {
    val event = JumpInfo(
      line = line,
      col = col,
      filepath = editor.virtualFile!!.path,
      protocol = "file",
      added = true,
      timestamp = System.currentTimeMillis() + 10_000,
    )
    ApplicationManager.getApplication().invokeAndWait {
      JumpRemoteTopicListener().handleEvent(fixture.project, event)
    }
  }

  @Test
  fun `test split showing a different file also inherits the jump list when ideawindowjumps is set`() {
    val mainWindow = configureMainWindow()
    enterCommand("set ideawindowjumps")
    enterSearch("sodden")
    enterSearch("shape")

    val mainWindowPath = mainWindow.virtualFile!!.path
    // Vim's `:vsplit {file}` copies the jump list of the window it splits, whichever buffer ends up in the new window
    var otherFile: VirtualFile? = null
    ApplicationManager.getApplication().invokeAndWait {
      otherFile = fixture.createFile("bbb.txt", "lorem ipsum\n")
    }
    val splitWindow = openSplitWindow(mainWindow, otherFile)
    selectWindow(splitWindow)

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   2     1    8 $mainWindowPath
        |   1     3   29 $mainWindowPath
        |>
      """.trimMargin(),
    )
  }

  /**
   * Stops IDE navigation from being mirrored into the jump list
   *
   * The platform's "place" for opening a file reaches us asynchronously, so asserting that a window's list is empty
   * would race with it. Mirroring has its own tests.
   */
  private fun disableIdeNavigationMirroring() {
    enterCommand("set noideaunifyjumps")
  }
}

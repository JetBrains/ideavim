/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.JumpRemoteTopicListener
import com.maddyhome.idea.vim.group.jump.JumpInfo
import org.jdom.Element
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimSplitWindowTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What gets written to `vim_settings_local.xml` when the jump list is scoped to a window
 *
 * Unlike the rest of the jump list tests, these reach into the service instead of driving `:`-commands: persistence
 * lives in a `PersistentStateComponent` and is not observable from Vim. IDE navigation is fed straight to
 * [JumpRemoteTopicListener] rather than through the platform, because the real event depends on RPC timing and would
 * make the test flaky.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "Persistence of the jump list is an IdeaVim concern. Vim writes its jump list to viminfo, which the " +
    "test framework does not read.",
)
class WindowJumpsPersistenceTest : VimSplitWindowTestCase() {
  private fun configureMainWindow(): Editor {
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
      editor = configureByText(
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
    return editor!!
  }

  /**
   * Feeds an IDE navigation event to the listener that implements the `unifyjumps` sync
   *
   * The timestamp has to be ahead of [com.maddyhome.idea.vim.api.VimJumpService.lastJumpTimeStamp], which Vim's own
   * jumps push into the future to suppress the platform's echo of them.
   */
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

  /** Builds the saved state of a jump list for the current project, in the format [persistedJumps] reads back */
  private fun savedState(editor: Editor, vararg positions: Pair<Int, Int>): Element {
    val projectElement = Element("project").setAttribute("id", injector.file.getProjectId(fixture.project))
    for ((line, col) in positions) {
      projectElement.addContent(
        Element("jump")
          .setAttribute("line", line.toString())
          .setAttribute("column", col.toString())
          .setAttribute("filename", editor.virtualFile!!.path)
          .setAttribute("protocol", "file"),
      )
    }
    return Element("projects").addContent(projectElement)
  }

  private fun loadSavedState(state: Element) {
    ApplicationManager.getApplication().invokeAndWait {
      @Suppress("UNCHECKED_CAST")
      (injector.jumpService as PersistentStateComponent<Element>).loadState(state)
    }
  }

  /** The persisted jumps of every project in the saved state, as "line:col" */
  private fun persistedJumps(): List<List<String>> {
    var state: Element? = null
    ApplicationManager.getApplication().invokeAndWait {
      @Suppress("UNCHECKED_CAST")
      state = (injector.jumpService as PersistentStateComponent<Element>).state
    }
    return state!!.getChildren("project").map { project ->
      project.getChildren("jump").map { "${it.getAttributeValue("line")}:${it.getAttributeValue("column")}" }
    }
  }

  @Test
  fun `test IDE navigation is persisted as part of the window list`() {
    val mainWindow = configureMainWindow()
    enterCommand("set windowjumps")
    enterSearch("sodden")
    enterSearch("shape")

    recordPlatformJump(mainWindow, line = 5, col = 3)

    // The IDE's jump joins the window's list, and that list is what gets saved. Recorded against the project instead, it
    // would be saved as a list of its own. Not asserted exactly: the platform also mirrors the file being opened, and
    // when that arrives is not something the test controls
    val persisted = persistedJumps().single()
    assertTrue(persisted.containsAll(listOf("0:8", "2:29")), "The window's own jumps should be saved: $persisted")
    assertTrue(persisted.contains("5:3"), "The IDE navigation should be saved with them: $persisted")
  }

  @Test
  fun `test project wide list is persisted when windowjumps is not set`() {
    configureMainWindow()
    enterSearch("sodden")
    enterSearch("shape")

    // With the option reset, the project id *is* the scope of the real jump list - it must still be saved
    assertEquals(listOf(listOf("0:8", "2:29")), persistedJumps())
  }

  @Test
  fun `test window with no jumps of its own shows the list restored from disk`() {
    val mainWindow = configureMainWindow()
    enterCommand("set windowjumps")

    // State is read at startup, before any window exists, so it can only be stored per project. A window that has no
    // list of its own has to inherit it - the same way a window inherits the list of the window it was split from
    loadSavedState(savedState(mainWindow, 0 to 8, 2 to 29))

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
  fun `test only the most recently used window's list is saved for a project`() {
    val mainWindow = configureMainWindow()
    enterCommand("set windowjumps")
    enterSearch("sodden")
    enterSearch("shape")

    val splitWindow = openSplitWindow(mainWindow)
    selectWindow(splitWindow)
    typeText("G")
    enterSearch("torrent")

    // One list per project, taken from the window used last. Window ids mean nothing after a restart, so there is no
    // point saving one list per window
    assertEquals(listOf(listOf("0:8", "2:29", "6:12", "8:0")), persistedJumps())
  }

  @Test
  fun `test window that jumps before reading its list still inherits the restored one`() {
    val mainWindow = configureMainWindow()
    enterCommand("set windowjumps")
    loadSavedState(savedState(mainWindow, 0 to 8, 2 to 29))

    // The first thing this window does is jump, without ever reading its list first - which is the normal case after a
    // restart. `j` is not a jump, it just moves off the restored positions so the new entry is distinguishable
    typeText("j")
    enterSearch("torrent")

    assertCommandOutput(
      "jumps",
      """ jump line  col file/text
        |   3     1    8 I found it in a legendary land
        |   2     3   29 where it was settled on some sodden sand
        |   1     2    8 all rocks and lavender and tufted grass,
        |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test clearjumps in a window that has not read its list is not undone by the restored list`() {
    val mainWindow = configureMainWindow()
    enterCommand("set windowjumps")
    loadSavedState(savedState(mainWindow, 0 to 8, 2 to 29))

    // The window has never read or written its own list, so the only list around is the project's. Clearing has to be
    // remembered, or the next read inherits the very entries that were just cleared
    enterCommand("clearjumps")

    assertCommandOutput("jumps", " jump line  col file/text\n>")
  }
}

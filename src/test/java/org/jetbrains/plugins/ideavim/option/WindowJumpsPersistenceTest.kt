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
}

/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.fugitive

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.QuickFixItem
import com.maddyhome.idea.vim.api.QuickFixJump
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.QuickFixProjectCloseListener
import com.maddyhome.idea.vim.newapi.IjVimEditor
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Every project (IDE window) has its own quickfix list, as if each were a separate Vim:
 * `:Ggrep` in one project doesn't give another project anything to walk with `:cnext`.
 *
 * The light fixture has a single project, so the other project is faked by an editor
 * that reports a different [VimEditor.projectId].
 */
class FugitiveQuickFixPerProjectTest : FugitiveTestCase() {

  @Test
  fun `test list from another project is empty`() {
    grepTwoMatches()

    val jump = cnextFromOtherProject()

    assertEquals(QuickFixJump.EmptyList, jump)
  }

  @Test
  fun `test jump from another project does not move this project's list`() {
    grepTwoMatches()
    cnextFromOtherProject()

    enterCommandInCurrentEditor("cnext")

    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test new list in another project does not move this project's position`() {
    grepTwoMatches()
    addFile("b.txt", "foo in b\n")
    git.respondWith("aaa.txt:1:7:first foo", "aaa.txt:3:5:  third foo", "b.txt:1:1:foo in b")
    enterCommandInCurrentEditor("Ggrep foo")
    enterCommandInCurrentEditor("cnext")
    onEdt { injector.quickFixService.replaceList(editorFromOtherProject(), listOf(OTHER_PROJECT_ITEM)) }

    enterCommandInCurrentEditor("cnext")

    assertCurrentFile("b.txt")
  }

  @Test
  fun `test closing the project forgets its list`() {
    grepTwoMatches()

    QuickFixProjectCloseListener().projectClosed(fixture.project)
    enterCommandInCurrentEditor("cnext")

    assertPluginError(true)
    assertPluginErrorMessage("E42: No Errors")
  }

  private fun grepTwoMatches() {
    configureByText(
      """
        |${c}first foo
        |second line
        |  third foo
      """.trimMargin()
    )
    git.respondWith("aaa.txt:1:7:first foo", "aaa.txt:3:5:  third foo")
    enterCommand("Ggrep foo")
    assertCurrentPosition(0, 6)
  }

  private fun cnextFromOtherProject(): QuickFixJump = onEdt {
    injector.quickFixService.jumpForward(editorFromOtherProject(), 1)
  }

  private fun <T> onEdt(action: () -> T): T {
    var result: T? = null
    ApplicationManager.getApplication().invokeAndWait { result = action() }
    @Suppress("UNCHECKED_CAST")
    return result as T
  }

  /**
   * The current editor, reporting another project. It must stay an [IjVimEditor], which the jump list relies on.
   */
  private fun editorFromOtherProject(): VimEditor = object : IjVimEditor(currentEditor()) {
    override val projectId: String = "other-project"
  }

  private companion object {
    val OTHER_PROJECT_ITEM = QuickFixItem(0, 0, "/elsewhere/x.txt", "file", "x")
  }
}

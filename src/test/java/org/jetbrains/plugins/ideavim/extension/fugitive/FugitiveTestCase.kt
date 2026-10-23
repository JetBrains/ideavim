/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.fugitive

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.git.GitClient
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Base for fugitive tests: enables the extension and replaces git with [FakeGitClient],
 * so each test states what git would print. The fake work tree is `/src`, where the
 * light fixture puts its files.
 *
 * `:Ggrep` and the quickfix commands switch editors, so the helpers here act on the
 * selected editor rather than the one the test was configured with.
 */
abstract class FugitiveTestCase : VimTestCase() {

  @TestDisposable
  lateinit var disposable: Disposable

  protected val git = FakeGitClient()

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    fixture.project.replaceService(GitClient::class.java, git, disposable)
    enableExtensions("fugitive")
  }

  /**
   * Greps two matches in `aaa.txt`, two in `b.txt` and one in `c.txt`, and lands on the first:
   *
   * ```
   * 1. aaa.txt (0, 6)  first foo
   * 2. aaa.txt (2, 4)  third foo
   * 3. b.txt   (0, 0)  foo b1
   * 4. b.txt   (1, 0)  foo b2
   * 5. c.txt   (0, 2)  x foo c
   * ```
   */
  protected fun grepFiveMatches() {
    configureByText(
      """
        |${c}first foo
        |second line
        |  third foo
      """.trimMargin()
    )
    addFile("b.txt", "foo b1\nfoo b2\n")
    addFile("c.txt", "x foo c\n")
    git.respondWith(
      "aaa.txt:1:7:first foo",
      "aaa.txt:3:5:  third foo",
      "b.txt:1:1:foo b1",
      "b.txt:2:1:foo b2",
      "c.txt:1:3:x foo c",
    )
    enterCommand("Ggrep foo")
    assertCurrentPosition(0, 6)
  }

  protected fun addFile(path: String, content: String) {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.addFileToProject(path, content)
    }
  }

  /**
   * [typeText] types into the editor the test was configured with, which is no longer selected after a jump.
   */
  protected fun typeInCurrentEditor(keys: String) {
    ApplicationManager.getApplication().invokeAndWait {
      typeText(currentEditor(), injector.parser.parseKeys(keys))
    }
  }

  protected fun enterCommandInCurrentEditor(command: String) {
    typeInCurrentEditor(":$command<CR>")
  }

  protected fun currentEditor(): Editor {
    val editor = FileEditorManager.getInstance(fixture.project).selectedTextEditor
    assertNotNull(editor, "No editor is selected")
    return editor
  }

  protected fun assertCurrentFile(name: String) {
    assertEquals(name, currentEditor().virtualFile?.name)
  }

  protected fun assertCurrentPosition(line: Int, column: Int) {
    assertEquals(LogicalPosition(line, column), currentEditor().caretModel.logicalPosition)
  }
}

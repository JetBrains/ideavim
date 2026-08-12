/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.autocmd

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests `:stopinsert` in an autocommand, which is how it is meant to be used (`:help :stopinsert` uses
 * `:au BufEnter scratch stopinsert` as its example).
 *
 * This is the reported scenario: a user wants to always land in Normal mode after switching files. Vim has no option for
 * this - the answer upstream is an autocommand.
 *
 * Note that mode in IdeaVim is global (like Vim's `State`), so it does not matter which editor the mode is asserted on.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class StopInsertAutoCmdTest : VimTestCase() {

  private lateinit var fileEditorManager: FileEditorManagerImpl
  private lateinit var fileA: Editor
  private lateinit var fileB: Editor

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager =
      FileEditorManagerImpl(
        fixture.project,
        (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "StopInsertAutoCmdTestScope")
      )
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)

    fileB = openBuffer("bbb.txt")
    ApplicationManager.getApplication().invokeAndWait {
      fileA = configureByText("abcdef")
    }

    // Start each test with a clean autocmd list
    enterCommand("autocmd!")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    try {
      enterCommand("autocmd!")
    } finally {
      super.tearDown(testInfo)
    }
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Test
  fun `test stopinsert on BufEnter forces normal mode when switching file`() {
    enterCommand("autocmd BufEnter * stopinsert")

    keys(fileA, "i")
    select(fileB)

    assertPluginError(false)
    assertEquals(Mode.NORMAL(), modeOf(fileB))
  }

  @Test
  fun `test stopinsert on WinEnter forces normal mode when switching file`() {
    enterCommand("autocmd WinEnter * stopinsert")

    keys(fileA, "i")
    select(fileB)

    assertPluginError(false)
    assertEquals(Mode.NORMAL(), modeOf(fileB))
  }

  @Test
  fun `test stopinsert on BufEnter forces normal mode when returning to a file left in insert mode`() {
    enterCommand("autocmd BufEnter * stopinsert")

    keys(fileA, "i")
    select(fileB)
    select(fileA)

    assertPluginError(false)
    assertEquals(Mode.NORMAL(), modeOf(fileA))
  }

  @Test
  fun `test stopinsert on BufEnter forces normal mode from replace mode`() {
    enterCommand("autocmd BufEnter * stopinsert")

    keys(fileA, "2l", "Rxy")
    select(fileB)

    assertPluginError(false)
    assertEquals(Mode.NORMAL(), modeOf(fileB))
    assertEquals("abxyef", textOf(fileA))
  }

  /**
   * An autocommand that does not ask to leave Insert mode must not leave it.
   *
   * Vim keeps Insert mode when the window changes without the user leaving Insert mode (see `ins_mouse` in Neovim's
   * `mouse.c`), and running `:echo` from an autocommand does not change the mode. Forcing Normal mode has to be opt-in,
   * via `:stopinsert` - otherwise `:stopinsert` has nothing to do.
   */
  @Test
  fun `test autocmd that does not stop insert mode keeps insert mode`() {
    enterCommand("autocmd BufEnter * echo \"entered\"")

    keys(fileA, "i")
    select(fileB)

    assertPluginError(false)
    assertEquals(Mode.INSERT, modeOf(fileB))
  }

  private fun openBuffer(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, "lorem ipsum"))
    }
    return fixture.editor
  }

  /**
   * Selects [editor]'s file, as if the user had clicked its tab, and waits for IdeaVim to finish handling the switch.
   *
   * Waiting on the selected file is not enough: `FileEditorManager` updates it before it publishes the event, so the
   * autocmd handlers may not have run yet. IdeaVim records the last selected editor as the very last statement of
   * `VimFileEditorManagerListener.selectionChanged`, so seeing that update means the events for this switch are done.
   * The tracker is reset first, so a stale value from an earlier switch can't satisfy the wait.
   */
  private fun select(editor: Editor) {
    val file = editor.virtualFile!!
    ApplicationManager.getApplication().invokeAndWait {
      VimListenerManager.VimLastSelectedEditorTracker.resetLastSelectedEditor(fixture.project)
      fileEditorManager.openFile(file, true)
    }
    val handled = waitUntil {
      var done = false
      ApplicationManager.getApplication().invokeAndWait {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        done = VimListenerManager.VimLastSelectedEditorTracker
          .getLastSelectedEditor(fixture.project)?.virtualFile == file
      }
      done
    }
    assertTrue(handled, "the selection change to ${file.name} was not delivered")
  }

  private fun keys(editor: Editor, vararg keys: String) {
    ApplicationManager.getApplication().invokeAndWait {
      typeText(editor, keys.flatMap { injector.parser.parseKeys(it) })
    }
  }

  private fun modeOf(editor: Editor): Mode {
    lateinit var mode: Mode
    ApplicationManager.getApplication().invokeAndWait { mode = editor.vim.mode }
    return mode
  }

  private fun textOf(editor: Editor): String {
    lateinit var text: String
    ApplicationManager.getApplication().invokeAndWait { text = editor.document.text }
    return text
  }

  private fun caretOffsetOf(editor: Editor): Int {
    var offset = -1
    ApplicationManager.getApplication().invokeAndWait { offset = editor.caretModel.offset }
    return offset
  }
}

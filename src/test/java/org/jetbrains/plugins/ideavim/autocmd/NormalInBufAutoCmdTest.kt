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
 * `:normal` runs its keys as Normal mode commands whatever mode the user is in, and restores that mode afterwards
 * (Vim's `ex_normal`). Keeping Insert mode across the file switch is deliberate - see [StopInsertAutoCmdTest].
 */
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class NormalInBufAutoCmdTest : VimTestCase() {

  private lateinit var fileEditorManager: FileEditorManagerImpl
  private lateinit var fileA: Editor
  private lateinit var fileB: Editor

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager =
      FileEditorManagerImpl(
        fixture.project,
        (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "NormalInBufAutoCmdTestScope")
      )
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)

    fileB = openBuffer("bbb.txt")
    ApplicationManager.getApplication().invokeAndWait {
      fileA = configureByText("abcdef")
    }

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
  fun `test normal in BufEnter autocmd runs its keys from normal mode`() {
    enterCommand("autocmd BufEnter * normal! x")

    select(fileB)

    assertPluginError(false)
    assertEquals("orem ipsum", textOf(fileB))
    assertEquals(Mode.NORMAL(), modeOf(fileB))
  }

  @Test
  fun `test normal in BufEnter autocmd runs its keys as commands while in insert mode`() {
    enterCommand("autocmd BufEnter * normal! x")

    keys(fileA, "i")
    select(fileB)

    assertPluginError(false)
    assertEquals("orem ipsum", textOf(fileB))
    assertEquals(Mode.INSERT, modeOf(fileB))
  }

  @Test
  fun `test normal in WinEnter autocmd runs its keys as commands while in insert mode`() {
    enterCommand("autocmd WinEnter * normal! x")

    keys(fileA, "i")
    select(fileB)

    assertPluginError(false)
    assertEquals("orem ipsum", textOf(fileB))
    assertEquals(Mode.INSERT, modeOf(fileB))
  }

  private fun openBuffer(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, "lorem ipsum"))
    }
    return fixture.editor
  }

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

  private fun textOf(editor: Editor): String {
    lateinit var text: String
    ApplicationManager.getApplication().invokeAndWait { text = editor.document.text }
    return text
  }

  private fun modeOf(editor: Editor): Mode {
    lateinit var mode: Mode
    ApplicationManager.getApplication().invokeAndWait { mode = editor.vim.mode }
    return mode
  }
}

/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.SelectLastFileCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LastCommandTest : VimTestCase() {

  private lateinit var fileEditorManager: FileEditorManagerImpl

  private val uiSettings get() = UISettings.getInstance()
  private var savedReuseNotModifiedTabs = false
  private var savedOpenTabsAtTheEnd = false

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager =
      FileEditorManagerImpl(
        fixture.project,
        (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "LastCommandTestScope"),
      )
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)

    // Make the tab order deterministic. Otherwise the test is flaky (especially on CI):
    // - reuseNotModifiedTabs would replace the current unmodified tab when opening the next file, so not all files
    //   would end up open.
    // - openTabsAtTheEnd=false inserts new tabs next to the current one, and since the "current" tab is updated
    //   asynchronously, the resulting order of `openFiles` (which `:last` indexes into) would vary between runs.
    savedReuseNotModifiedTabs = uiSettings.reuseNotModifiedTabs
    savedOpenTabsAtTheEnd = uiSettings.openTabsAtTheEnd
    uiSettings.reuseNotModifiedTabs = false
    uiSettings.openTabsAtTheEnd = true
  }

  @AfterEach
  fun restoreUiSettings() {
    uiSettings.reuseNotModifiedTabs = savedReuseNotModifiedTabs
    uiSettings.openTabsAtTheEnd = savedOpenTabsAtTheEnd
  }

  // A heavy fixture is required: FileEditorManagerImpl (used to open several files below) is forbidden in light tests.
  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("last")
    assertTrue(command is SelectLastFileCommand)
  }

  // Regression test for `:last`: the command used to pass 999 to selectFile() while the implementation
  // only recognised 99 as the "select last file" sentinel, so `:last` silently did nothing (it returned
  // an error) for any realistic number of open files. This test fails with the old (buggy) implementation
  // because the selection never moves to the last file, and passes once the values agree.
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `last selects the last open file`() {
    openFile("aaa.txt")
    openFile("bbb.txt")
    val lastFile = openFile("ccc.txt")
    waitForCondition("Test setup: all three files should be open") { fileEditorManager.openFiles.size == 3 }

    // Move the selection away from the last file so that `:last` has something to do
    selectFile(fileEditorManager.openFiles.first())
    waitForCondition("Test setup: should not start on the last file") {
      fileEditorManager.selectedFiles.firstOrNull().let { it != null && it != lastFile }
    }
    assertNotEquals(lastFile, fileEditorManager.selectedFiles.first(), "Test setup: should not start on the last file")

    enterCommand("last")
    waitForCondition("`:last` should select the last open file") {
      fileEditorManager.selectedFiles.firstOrNull() == lastFile
    }

    assertEquals(lastFile, fileEditorManager.selectedFiles.first())
    assertEquals(fileEditorManager.openFiles.last(), fileEditorManager.selectedFiles.first())
  }

  private fun openFile(filename: String): VirtualFile {
    var file: VirtualFile? = null
    ApplicationManager.getApplication().invokeAndWait {
      file = fixture.createFile(filename, "lorem ipsum")
      fixture.openFileInEditor(file!!)
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    return file!!
  }

  private fun selectFile(file: VirtualFile) {
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.openFile(file, true)
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }

  /**
   * Opening and selecting files through [FileEditorManagerImpl] completes asynchronously (via its coroutine scope and
   * focus events), so reading [FileEditorManagerImpl.selectedFiles] / [FileEditorManagerImpl.openFiles] right away can
   * observe stale state. Pump the event queue until the expected state is reached (or we give up), instead of racing it.
   */
  private fun waitForCondition(message: String, condition: () -> Boolean) {
    ApplicationManager.getApplication().invokeAndWait {
      repeat(100) {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        if (condition()) return@invokeAndWait
      }
      assertTrue(condition(), message)
    }
  }
}

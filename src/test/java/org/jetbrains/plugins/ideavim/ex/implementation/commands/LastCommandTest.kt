/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.SelectLastFileCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LastCommandTest : VimTestCase() {

  private lateinit var fileEditorManager: FileEditorManagerImpl

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager =
      FileEditorManagerImpl(
        fixture.project,
        (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "LastCommandTestScope"),
      )
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)
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

    // Move the selection away from the last file so that `:last` has something to do
    selectFile(fileEditorManager.openFiles.first())
    assertNotEquals(lastFile, fileEditorManager.selectedFiles.first(), "Test setup: should not start on the last file")

    enterCommand("last")

    assertEquals(lastFile, fileEditorManager.selectedFiles.first())
    assertEquals(fileEditorManager.openFiles.last(), fileEditorManager.selectedFiles.first())
  }

  private fun openFile(filename: String): VirtualFile {
    var file: VirtualFile? = null
    ApplicationManager.getApplication().invokeAndWait {
      file = fixture.createFile(filename, "lorem ipsum")
      fixture.openFileInEditor(file!!)
    }
    return file!!
  }

  private fun selectFile(file: VirtualFile) {
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.openFile(file, true)
    }
  }
}

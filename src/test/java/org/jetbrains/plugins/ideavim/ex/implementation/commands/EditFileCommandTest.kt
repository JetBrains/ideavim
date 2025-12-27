/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.EditFileCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EditFileCommandTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    // Replace test FileEditorManager with real implementation to support opening LightVirtualFile
    val manager = FileEditorManagerImpl(
      fixture.project,
      (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "EditFileCommandTest")
    )
    fixture.project.replaceService(FileEditorManager::class.java, manager, fixture.testRootDisposable)
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("edit ~/.ideavimrc")
    assertTrue(command is EditFileCommand)
    assertEquals("~/.ideavimrc", command.argument)
  }

  @Test
  fun `command parsing 2`() {
    val command = injector.vimscriptParser.parseCommand("browse ~/.ideavimrc")
    assertTrue(command is EditFileCommand)
    assertEquals("~/.ideavimrc", command.argument)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test edit creates buffer for non-existent file`() {
    configureByText("existing content")
    // Use a unique filename to avoid accidentally matching existing files
    enterCommand("edit ThisFileDoesNotExist12345.txt")

    // Verify a new buffer was opened by checking the selected editor
    val fileManager = FileEditorManager.getInstance(fixture.project)
    val selectedEditor = fileManager.selectedTextEditor
    assertNotNull(selectedEditor, "A new editor should be opened")

    // Get the file associated with the selected editor's document
    val selectedFile = FileDocumentManager.getInstance().getFile(selectedEditor.document)
    assertNotNull(selectedFile, "A file should be associated with the selected editor")
    assertEquals("ThisFileDoesNotExist12345.txt", selectedFile.name)
    assertTrue(selectedFile is LightVirtualFile)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test edit creates empty buffer for non-existent file`() {
    configureByText("existing content")
    enterCommand("edit NewFile.txt")

    // Verify a new buffer was opened
    val fileManager = FileEditorManager.getInstance(fixture.project)
    val selectedEditor = fileManager.selectedTextEditor

    // The new buffer should be empty
    assertNotNull(selectedEditor, "An editor should be selected")
    assertEquals("", selectedEditor.document.text)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test edit non-existent java file`() {
    configureByText("existing content")
    enterCommand("edit UniqueTestFile9999.java")

    // Verify a new buffer was opened by checking the selected editor
    val fileManager = FileEditorManager.getInstance(fixture.project)
    val selectedEditor = fileManager.selectedTextEditor
    assertNotNull(selectedEditor, "A new editor should be opened")

    // The new buffer should be empty
    assertEquals("", selectedEditor.document.text)

    // Get the file associated with the selected editor's document
    val selectedFile = FileDocumentManager.getInstance().getFile(selectedEditor.document)
    assertNotNull(selectedFile, "A file should be associated with the selected editor")
    assertEquals("UniqueTestFile9999.java", selectedFile.name)
  }
}
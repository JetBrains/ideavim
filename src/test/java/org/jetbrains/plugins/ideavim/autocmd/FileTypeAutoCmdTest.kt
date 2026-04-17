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
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class FileTypeAutoCmdTest : VimTestCase() {

  private lateinit var fileEditorManager: FileEditorManagerImpl

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager =
      FileEditorManagerImpl(
        fixture.project,
        (fixture.project as ComponentManagerEx)
          .getCoroutineScope()
          .childScope(name = "FileTypeAutoCmdTestScope")
      )
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)

    ApplicationManager.getApplication().invokeAndWait {
      configureByText("\n")
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
  fun `should fire FileType when opening a file`() {
    enterCommand("autocmd FileType text echo \"text-file\"")
    openFile("hello.txt")
    assertExOutput("text-file")
  }

  @Test
  fun `should match FileType pattern against filetype name not file path`() {
    // Pattern `*.txt` matches file paths, not filetype names, so it should NOT fire
    enterCommand("autocmd FileType *.txt echo \"path\"")
    openFile("hello.txt")
    assertNoExOutput()
  }

  @Test
  fun `should match FileType with wildcard pattern`() {
    enterCommand("autocmd FileType * echo \"any\"")
    openFile("hello.txt")
    assertExOutput("any")
  }

  @Test
  fun `should match FileType with alternation pattern`() {
    enterCommand("autocmd FileType {text,python} echo \"matched\"")
    openFile("hello.txt")
    assertExOutput("matched")
  }

  @Test
  fun `should not fire FileType for non-matching filetype`() {
    enterCommand("autocmd FileType python echo \"py\"")
    openFile("hello.txt")
    assertNoExOutput()
  }

  private fun openFile(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, "lorem ipsum"))
    }
    return fixture.editor
  }
}

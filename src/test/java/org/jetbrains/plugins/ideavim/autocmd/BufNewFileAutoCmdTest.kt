/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.autocmd

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
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
class BufNewFileAutoCmdTest : VimTestCase() {

  private lateinit var fileEditorManager: FileEditorManagerImpl

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager =
      FileEditorManagerImpl(
        fixture.project,
        (fixture.project as ComponentManagerEx)
          .getCoroutineScope()
          .childScope(name = "BufNewFileAutoCmdTestScope")
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
  fun `should fire BufNewFile when creating and opening a new file`() {
    enterCommand("autocmd BufNewFile * echo \"new\"")
    openNewFile("fresh.txt")
    assertExOutput("new")
  }

  @Test
  fun `should not fire BufRead for a newly created file`() {
    enterCommand("autocmd BufNewFile * echo \"new\"")
    enterCommand("autocmd BufRead * echo \"read\"")
    openNewFile("fresh.txt")
    // Vim semantics: only BufNewFile fires for new files, BufRead is suppressed
    assertExOutput("new")
  }

  @Test
  fun `should fire FileType alongside BufNewFile`() {
    enterCommand("autocmd BufNewFile * echo \"1-new\"")
    enterCommand("autocmd FileType * echo \"2-filetype\"")
    openNewFile("fresh.txt")
    assertExOutput("1-new\n2-filetype")
  }

  @Test
  fun `should match BufNewFile against file pattern`() {
    enterCommand("autocmd BufNewFile *.py echo \"py\"")
    openNewFile("fresh.txt")
    assertNoExOutput()
  }

  private fun openNewFile(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      val parent = fixture.tempDirFixture.getFile(".") ?: error("temp dir unavailable")
      val file = WriteCommandAction.runWriteCommandAction<com.intellij.openapi.vfs.VirtualFile>(fixture.project) {
        parent.createChildData(this, filename).apply { setBinaryContent("lorem ipsum".toByteArray()) }
      }
      fixture.openFileInEditor(file)
    }
    return fixture.editor
  }
}

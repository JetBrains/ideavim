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
import com.maddyhome.idea.vim.listener.BufNewFileTracker
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class BufReadAutoCmdTest : VimTestCase() {

  private lateinit var fileEditorManager: FileEditorManagerImpl

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager =
      FileEditorManagerImpl(
        fixture.project,
        (fixture.project as ComponentManagerEx)
          .getCoroutineScope()
          .childScope(name = "BufReadAutoCmdTestScope")
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
  fun `should fire BufRead when opening a file`() {
    enterCommand("autocmd BufRead * echo \"read\"")
    openFile("hello.txt")
    assertExOutput("read")
  }

  @Test
  fun `should fire BufReadPost when opening a file`() {
    enterCommand("autocmd BufReadPost * echo \"post\"")
    openFile("hello.txt")
    assertExOutput("post")
  }

  @Test
  fun `should match BufRead against file extension`() {
    enterCommand("autocmd BufRead *.txt echo \"txt\"")
    openFile("hello.txt")
    assertExOutput("txt")
  }

  @Test
  fun `should not fire BufRead for non-matching pattern`() {
    enterCommand("autocmd BufRead *.py echo \"py\"")
    openFile("hello.txt")
    assertNoExOutput()
  }

  @Test
  fun `should fire BufRead BufReadPost and FileType in vim order`() {
    // Vim order for opening an existing file: BufRead == BufReadPost → FileType → BufEnter
    enterCommand("autocmd BufRead * echo \"1-read\"")
    enterCommand("autocmd BufReadPost * echo \"2-readpost\"")
    enterCommand("autocmd FileType * echo \"3-filetype\"")
    openFile("hello.txt")
    assertExOutput("1-read\n2-readpost\n3-filetype")
  }

  private fun openFile(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      val file = fixture.createFile(filename, "lorem ipsum")
      // Simulate opening an existing (already on-disk) file: clear the "newly created"
      // marker so the open fires BufRead/BufReadPost instead of BufNewFile.
      BufNewFileTracker.consumeIfNew(file.path)
      fixture.openFileInEditor(file)
    }
    return fixture.editor
  }
}

/*
 * Copyright 2003-2025 The IdeaVim authors
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
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class BuffAutoCmdTest : VimTestCase() {

  private lateinit var fileEditorManager: FileEditorManagerImpl
  private lateinit var mainWindow: Editor
  private lateinit var otherBufferWindow: Editor
  private lateinit var splitWindow: Editor

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager =
      FileEditorManagerImpl(
        fixture.project,
        (fixture.project as ComponentManagerEx)
          .getCoroutineScope()
          .childScope(name = "BuffAutoCmdTestScope")
      )
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)

    // Create a new editor that will represent a new buffer in a separate window. It will have default values
    otherBufferWindow = openNewBufferWindow("bbb.txt")

    var curWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      // Create the original editor last, so that fixture.editor will point to this file
      // It is STRONGLY RECOMMENDED to use mainWindow instead of fixture.editor, so we know which editor we're using
      mainWindow = configureByText("\n")  // aaa.txt
      curWindow = fileEditorManager.currentWindow
    }

    curWindow.let {
      // Split the original editor into a new window, then reset the focus back to the originalEditor's EditorWindow
      // We do this before setting any custom state, so it will have default values for everything
      splitWindow = openSplitWindow(mainWindow) // aaa.txt
      fileEditorManager.currentWindow = it
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
  fun `should support BuffEnter event`() {
    enterCommand("autocmd BuffEnter * echo 2")
    openNewBufferWindow("test.txt")
    assertExOutput("2")
  }

  @Test
  fun `should support BuffExit event`() {
    enterCommand("autocmd BuffLeave * echo 3")
    closeWindow(otherBufferWindow)
    assertExOutput("3")
  }

  private fun openNewBufferWindow(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, "lorem ipsum"))
    }
    return fixture.editor
  }

  private fun openSplitWindow(editor: Editor): Editor {
    var splitWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileEditorManager.currentWindow
      splitWindow = currentWindow!!.split(
        SwingConstants.VERTICAL,
        true,
        editor.virtualFile,
        false
      )
    }

    waitUntil {
      splitWindow!!.allComposites.first().selectedEditor != null
    }
    return (splitWindow!!.allComposites.first().selectedEditor as TextEditor).editor
  }

  /**
   * Closes the given editor
   */
  private fun closeWindow(editor: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      // Just using fileEditorManager.closeFile(editor.virtualFile) can cause weird side effects, like opening a
      // different buffer in an open editor. See FileGroup.closeFile
      // But we can't just rely on the current EditorWindow. E.g., if we're trying to close a file that's not currently
      // open in the current window, or is open in a split while we want to close the *other* editor...
      val editorWindow = fileEditorManager.windows.first { window ->
        window.allComposites.any { composite ->
          composite.allEditors
            .filterIsInstance<TextEditor>()
            .any { textEditor -> textEditor.editor == editor }
        }
      }
      val virtualFile = editor.virtualFile

      if (virtualFile != null) {
        editorWindow.closeFile(virtualFile)
        editorWindow.requestFocus(true)
      }
    }
  }
}

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
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class BuffAutoCmdTest : VimTestCase() {

  private lateinit var manager: FileEditorManagerImpl
  private lateinit var otherBufferWindow: Editor
  private lateinit var originalEditor: Editor
  private lateinit var splitWindow: Editor

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase to allow us to split windows
    manager =
      FileEditorManagerImpl(fixture.project, (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "BuffAutoCmdTestScope"))
    fixture.project.replaceService(FileEditorManager::class.java, manager, fixture.testRootDisposable)

    // Create a new editor that will represent a new buffer in a separate window. It will have default values
    otherBufferWindow = openNewBufferWindow("bbb.txt")

    ApplicationManager.getApplication().invokeAndWait {
      // Create the original editor last, so that fixture.editor will point to this file
      // It is STRONGLY RECOMMENDED to use originalEditor instead of fixture.editor, so we know which editor we're using
      originalEditor = configureByText("\n")  // aaa.txt
    }

    // Split the current window. Since no options have been set, it will have default values
    splitWindow = openSplitWindow(originalEditor) // aaa.txt
  }

  @Test
  fun `should support BuffEnter event`() {
    openNewBufferWindow("first.txt")
    enterCommand("autocmd BuffEnter * echo 2")
    openNewBufferWindow("test.txt")
    assertExOutput("2")
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  // Note that this overwrites fixture.editor! This is the equivalent of `:new {file}`
  private fun openNewBufferWindow(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, "lorem ipsum"))
    }
    return fixture.editor
  }

  private fun openSplitWindow(editor: Editor): Editor {
    val fileManager = FileEditorManagerEx.getInstanceEx(fixture.project)
    var split: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileManager.currentWindow
      split = currentWindow!!.split(
        SwingConstants.VERTICAL,
        true,
        editor.virtualFile,
        false
      )
    }

    // Waiting till the selected editor will appear
    waitUntil {
      split!!.allComposites.first().selectedEditor != null
    }

    return (split!!.allComposites.first().selectedEditor as TextEditor).editor
  }
}
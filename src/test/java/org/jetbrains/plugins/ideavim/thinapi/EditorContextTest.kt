/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.intellij.vim.api.VimApi
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimApiImpl
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants
import kotlin.test.assertEquals

/**
 * Tests that VimApi editor context correctly tracks window switching.
 *
 * Verifies that `getSelectedEditor(projectId)` reflects FileEditorManager's
 * internal state, so that after switching windows, `editor { read { ... } }`
 * operates on the newly selected editor.
 */
class EditorContextTest : VimTestCase() {
  private lateinit var fileEditorManager: FileEditorManagerImpl

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    fileEditorManager = FileEditorManagerImpl(
      fixture.project,
      (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "EditorContextTest")
    )
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)
  }

  private fun createVimApi(): VimApi {
    val projectId = injector.file.getProjectId(fixture.project)
    return VimApiImpl(
      ListenerOwner.Plugin.get("test"),
      MappingOwner.Plugin.get("test"),
      projectId
    )
  }

  @Test
  fun `test selectNextWindow changes editor context`() {
    // Open first file (3 lines)
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile("first.txt", "line1\nline2\nline3"))
    }

    // Split and open second file (5 lines) in the new window
    var splitWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileEditorManager.currentWindow
      splitWindow = currentWindow!!.split(
        SwingConstants.VERTICAL,
        true,
        fixture.editor.virtualFile,
        false
      )
    }
    waitUntil { splitWindow!!.allComposites.first().selectedEditor != null }
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile("second.txt", "a\nb\nc\nd\ne"))
    }

    val vimApi = createVimApi()
    assertEquals(5, vimApi.editor { read { lineCount } })

    // Window switching via the platform API has an async gap: setAsCurrentWindow()
    // updates _currentWindowFlow synchronously, but getSelectedTextEditor() reads
    // from currentCompositeFlow which is derived via flatMapLatest (async).
    // We need to wait for propagation. Tracked in IJPL-235369.
    val editorBefore = fileEditorManager.selectedTextEditor
    ApplicationManager.getApplication().invokeAndWait {
      val editor = fileEditorManager.selectedTextEditor!!
      val context = injector.executionContextManager.getEditorExecutionContext(editor.vim)
      injector.window.selectNextWindow(context)
    }
    waitUntil { fileEditorManager.selectedTextEditor != editorBefore }

    assertEquals(3, vimApi.editor { read { lineCount } })
  }

  @Test
  fun `test editor context tracks when user opens file in another window`() {
    // Open first file (3 lines)
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile("first.txt", "line1\nline2\nline3"))
    }

    val vimApi = createVimApi()
    assertEquals(3, vimApi.editor { read { lineCount } })

    // Split window and open a second file (5 lines) — simulates user opening
    // a file in another split, which makes it the active editor
    var splitWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileEditorManager.currentWindow
      splitWindow = currentWindow!!.split(
        SwingConstants.VERTICAL,
        true,
        fixture.editor.virtualFile,
        false
      )
    }
    waitUntil { splitWindow!!.allComposites.first().selectedEditor != null }
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile("second.txt", "a\nb\nc\nd\ne"))
    }

    // The same VimApi instance now reads from the second editor
    assertEquals(5, vimApi.editor { read { lineCount } })
  }
}

/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants

/**
 * Base class for tests that need more than one Vim window (i.e. more than one IntelliJ editor)
 *
 * The default test fixture cannot split windows, so we replace [FileEditorManager] with a real
 * [FileEditorManagerImpl] (as `FileEditorManagerTestCase` does), which in turn means we can't use the light project
 * descriptor.
 *
 * Two kinds of "new window" are available, and the difference matters for anything scoped to a Vim window:
 * * [openNewBufferWindow] opens a file in a new tab of the *current* split. Vim's equivalent is `:edit {file}` (see the
 *   note in [openNewBufferWindow]).
 * * [openSplitWindow] splits the current window, so the new editor lives in a *different* split.
 *
 * A test drives one window at a time: call [selectWindow] and then use the normal [VimTestCase] helpers. See
 * [selectWindow] for why typing directly into an unselected editor does not work.
 */
@Suppress("SameParameterValue")
abstract class VimSplitWindowTestCase : VimTestCase() {
  protected lateinit var fileEditorManager: FileEditorManagerImpl

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase to allow us to split windows
    fileEditorManager = FileEditorManagerImpl(
      fixture.project,
      (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = javaClass.simpleName),
    )
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)
  }

  // If we're replacing the test FileEditorManager, then we can't use the default light project descriptor
  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  /**
   * Opens a new buffer in a new tab of the current split, and moves the focus to it
   *
   * Vim's `:new {file}` splits the current window and edits the file in the new split, while `:edit {file}` reuses the
   * current window. IdeaVim doesn't support `:new {file}`, and its `:edit {file}` opens a new tab in the current split -
   * which is what this function does. The new editor is a new Vim *buffer*, but stays in the same split.
   *
   * Note that this overwrites `fixture.editor`!
   *
   * @return the `Editor` representing the new window
   */
  protected fun openNewBufferWindow(filename: String, content: String = "lorem ipsum"): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, content))
    }
    return fixture.editor
  }

  /**
   * Splits the given editor/Vim window vertically and moves the focus to the new editor
   *
   * Equivalent to `<C-W>v` or `:vsplit` with the `'splitright'` option enabled in Vim. (Note that IdeaVim doesn't
   * currently support `'splitright'` or `'splitbelow'`.)
   *
   * Pass [file] to open a different file in the new window, which is Vim's `:vsplit {file}`.
   *
   * @return the `Editor` representing the new window
   */
  protected fun openSplitWindow(editor: Editor, file: VirtualFile? = editor.virtualFile): Editor {
    // Open the split with the API, rather than Vim commands, so we get the editor
    var splitWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileEditorManager.currentWindow
      splitWindow = currentWindow!!.split(SwingConstants.VERTICAL, true, file, false)
    }

    // Waiting till the selected editor will appear
    waitUntil {
      splitWindow!!.allComposites.first().selectedEditor != null
    }
    return (splitWindow!!.allComposites.first().selectedEditor as TextEditor).editor
  }

  /**
   * Closes the window (split or tab) that owns the given editor
   */
  protected fun closeWindow(editor: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      // Just using fileEditorManager.closeFile(editor.virtualFile) can cause weird side effects, like opening a
      // different buffer in an open editor. See IjFileGroup.closeFile
      val virtualFile = editor.virtualFile ?: return@invokeAndWait
      val editorWindow = windowFor(editor) ?: return@invokeAndWait
      editorWindow.closeFile(virtualFile)
      editorWindow.requestFocus(true)
    }
  }

  /**
   * Runs an Ex command and returns what it printed
   *
   * For assertions that cannot be exact: the platform mirrors IDE navigation into the jump list asynchronously (see
   * `ideaunifyjumps`), so a list can legitimately carry entries a test never made.
   */
  protected fun commandOutput(command: String): String {
    clearOutputPanel()
    enterCommand(command)
    return readOutputPanel { it.text } ?: error("No Ex output for '$command'")
  }

  /**
   * Selects the window that owns the given editor, and points the test fixture at it
   *
   * All of [VimTestCase]'s helpers (`typeText`, `enterCommand`, `enterSearch`, `assertState`, `assertExOutput`, ...) act
   * on `fixture.editor`, and IdeaVim resolves the editor to act on from the *selected* window rather than from the
   * editor a keystroke was sent to. Typing into an editor that isn't selected therefore does the wrong thing - the keys
   * are applied to the selected editor, and Ex commands produce no output at all. That never happens in a real IDE,
   * because the user can only type into the focused window.
   *
   * Call this before driving a window, then use the normal [VimTestCase] helpers:
   * ```
   * val splitWindow = openSplitWindow(mainWindow)
   * selectWindow(splitWindow)
   * enterCommand("jumps")
   * ```
   */
  protected fun selectWindow(editor: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.currentWindow = windowFor(editor)
      // Re-point fixture.editor at this editor. The file is already open in this window, so this selects it rather than
      // opening anything new
      editor.virtualFile?.let { fixture.openFileInEditor(it) }
    }

    // Fail loudly rather than silently driving the wrong window
    check(fixture.editor === editor) { "Could not select the requested window. fixture.editor is a different editor" }
  }

  /**
   * The [EditorWindow] (split) that owns the given editor. Must be called on the EDT
   */
  private fun windowFor(editor: Editor): EditorWindow? {
    // We can't rely on the current EditorWindow. E.g., if we're looking for a file that's not currently open in the
    // current window, or is open in a split while we want the *other* editor
    return fileEditorManager.windows.find { window ->
      window.allComposites.any { composite ->
        composite.allEditors.filterIsInstance<TextEditor>().any { it.editor == editor }
      }
    }
  }
}

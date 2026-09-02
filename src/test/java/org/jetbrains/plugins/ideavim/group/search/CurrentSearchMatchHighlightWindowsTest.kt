/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.search

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants
import kotlin.test.assertContentEquals

/**
 * Tests the current match highlight (`‷…‴`, IdeaVim's equivalent of Vim's `hl-CurSearch`) with more than one window
 * open on the same buffer. See [SearchHighlightsTest] for the rule that applies throughout: the box is only drawn
 * while `'incsearch'` is previewing the pattern being typed.
 *
 * The box is therefore a property of the window doing the searching, and only while its command line is open. Once the
 * search is accepted or cancelled, no window draws it - neither the window that searched nor the others, however their
 * carets happen to be placed. Switching focus between windows must not change that, and must not recreate the
 * highlighters either: unlike Vim, which recomputes the screen on every redraw, IdeaVim's highlighters are persistent,
 * so removing and re-adding them on a focus change is visible as a flicker (VIM-4308).
 */
@TestWithoutNeovim(
  SkipNeovimReason.SEE_DESCRIPTION,
  description = "Drives two IDE editor windows directly; the Neovim test harness only mirrors a single window",
)
class CurrentSearchMatchHighlightWindowsTest : VimTestCase() {
  private lateinit var fileEditorManager: FileEditorManagerImpl
  private lateinit var mainWindow: Editor
  private lateinit var splitWindow: Editor

  // "one two one two one two" - the matches of "two" start at offsets 4, 12 and 20
  private val firstMatch = 4
  private val thirdMatch = 20

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase (via OptionDeclaredScopeTest) to allow us to split windows
    val scope = (fixture.project as ComponentManagerEx).getCoroutineScope()
    fileEditorManager = FileEditorManagerImpl(fixture.project, scope.childScope(name = "SearchHighlightWindowsTest"))
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)

    var currentWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      mainWindow = configureByText("${c}one two one two one two")
      currentWindow = fileEditorManager.currentWindow
    }

    // Split the main window, then put the focus back in the main window
    splitWindow = openSplitWindow(mainWindow)
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.currentWindow = currentWindow
    }

    enterCommand("set hlsearch")
  }

  // If we're replacing the test FileEditorManager, then we can't use the default light project descriptor
  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Test
  fun `test accepting a search shows no current match highlight in any window`() {
    // Both carets end up inside a match, and inside a different one each, so a highlight derived from the caret would
    // show up in both windows, at different offsets
    moveCaret(splitWindow, thirdMatch)

    enterSearch("two") // Leaves the main caret inside the first match

    assertSearchHighlights(mainWindow, "two", "one «two» one «two» one «two»")
    assertSearchHighlights(splitWindow, "two", "one «two» one «two» one «two»")
  }

  @Test
  fun `test switching focus shows no current match highlight in any window`() {
    moveCaret(splitWindow, thirdMatch)
    enterSearch("two")

    // Click into the other window
    switchFocusTo(splitWindow)

    assertSearchHighlights(mainWindow, "two", "one «two» one «two» one «two»")
    assertSearchHighlights(splitWindow, "two", "one «two» one «two» one «two»")

    // ...and back
    switchFocusTo(mainWindow)

    assertSearchHighlights(mainWindow, "two", "one «two» one «two» one «two»")
    assertSearchHighlights(splitWindow, "two", "one «two» one «two» one «two»")
  }

  @Test
  fun `test cancelling incsearch shows no current match highlight in any window`() {
    moveCaret(splitWindow, thirdMatch)
    enterCommand("set incsearch")
    enterSearch("two")

    typeText("/", "xyz", "<Esc>")

    assertSearchHighlights(mainWindow, "two", "one «two» one «two» one «two»")
    assertSearchHighlights(splitWindow, "two", "one «two» one «two» one «two»")
  }

  @Test
  fun `test incsearch previews the current match in the searching window only`() {
    moveCaret(splitWindow, thirdMatch)
    enterCommand("set incsearch")

    typeText("/", "two") // Leave the command line open, so the incsearch preview is active

    assertSearchHighlights(mainWindow, "two", "one ‷two‴ one «two» one «two»")
    assertSearchHighlights(splitWindow, "two", "one «two» one «two» one «two»")
  }

  @Test
  fun `test switching focus does not recreate the search highlights`() {
    moveCaret(splitWindow, firstMatch)
    enterSearch("two")

    val mainHighlighters = mainWindow.markupModel.allHighlighters.toList()
    val splitHighlighters = splitWindow.markupModel.allHighlighters.toList()

    switchFocusTo(splitWindow)

    assertContentEquals(
      mainHighlighters,
      mainWindow.markupModel.allHighlighters.toList(),
      "The main window's highlighters were recreated by the focus change",
    )
    assertContentEquals(
      splitHighlighters,
      splitWindow.markupModel.allHighlighters.toList(),
      "The split window's highlighters were recreated by the focus change",
    )
  }

  private fun openSplitWindow(editor: Editor): Editor {
    var splitWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileEditorManager.currentWindow
      splitWindow = currentWindow!!.split(SwingConstants.VERTICAL, true, editor.virtualFile, false)
    }
    waitUntil { splitWindow!!.allComposites.first().selectedEditor != null }
    return (splitWindow!!.allComposites.first().selectedEditor as TextEditor).editor
  }

  private fun switchFocusTo(editor: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.currentWindow = fileEditorManager.windows.first { window ->
        window.allComposites.any { composite ->
          composite.allEditors.filterIsInstance<TextEditor>().any { it.editor == editor }
        }
      }
    }
  }

  /** Move a window's caret the way a mouse click does, i.e. without giving that window the focus */
  private fun moveCaret(editor: Editor, offset: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      editor.caretModel.primaryCaret.moveToOffset(offset)
    }
  }
}

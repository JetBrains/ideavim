/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.stickyLines.StickyLinesModel
import com.intellij.testFramework.EditorTestUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.ScrollViewHelper
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * With scrolloff=999, the caret stays in the middle of the area below the sticky lines panel (VIM-4329).
 *
 * The panel height depends on the scroll position, so there isn't always an exact middle, and the caret can be a row
 * off. See [moveAndCheck] for what is checked after every move.
 */
@TestWithoutNeovim(SkipNeovimReason.OPTION)
class ScrolloffCentredCaretTest : VimTestCase() {
  @ParameterizedTest
  @ValueSource(strings = ["j", "k"])
  fun `test move scrolls one line without sticky lines`(motion: String) {
    withCentredCaret { editor ->
      typeText(editor, "330G")
      moveAndCheck(editor, motion, maxRowsOffCentre = 0, linesPerMove = 1)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["j", "k"])
  fun `test move scrolls one line with sticky lines panel of constant height`(motion: String) {
    withCentredCaret { editor ->
      addStickyLine(editor, 100, 900)
      addStickyLine(editor, 101, 900)
      typeText(editor, "330G")
      moveAndCheck(editor, motion, linesPerMove = 1)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["j", "k"])
  fun `test move keeps caret centred while sticky lines panel changes height`(motion: String) {
    withCentredCaret { editor ->
      addNestedScopes(editor)
      typeText(editor, "330G")
      moveAndCheck(editor, motion)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["j", "k"])
  fun `test move keeps caret centred with block inlays`(motion: String) {
    withCentredCaret { editor ->
      addDocComments(editor)
      typeText(editor, "330G")
      moveAndCheck(editor, motion)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["j", "k"])
  fun `test move keeps caret centred with block inlays and sticky lines`(motion: String) {
    withCentredCaret { editor ->
      addNestedScopes(editor)
      addDocComments(editor)
      typeText(editor, "330G")
      moveAndCheck(editor, motion)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["j", "k"])
  fun `test move keeps caret visible past block inlays that take up most of the screen`(motion: String) {
    withCentredCaret { editor ->
      addInlayAbove(editor, 315, screenHeight - 10)
      addInlayAbove(editor, 345, screenHeight - 10)
      typeText(editor, "330G")
      moveAndCheck(editor, motion, maxRowsOffCentre = null)
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `test moving down and back up returns to the same position with sticky lines`(withDocComments: Boolean) {
    withCentredCaret { editor ->
      addNestedScopes(editor)
      if (withDocComments) addDocComments(editor)
      typeText(editor, "330G")
      val start = screenState(editor)
      moveAndCheck(editor, "j")
      moveAndCheck(editor, "k")
      assertEquals(start, screenState(editor))
    }
  }

  // The jump centres the caret using the panel at the old position, the panel at the new position is different
  @ParameterizedTest
  @CsvSource("101, j", "101, k", "500, j", "500, k")
  fun `test first move after a jump scrolls with sticky lines`(jumpFrom: Int, motion: String) {
    withCentredCaret { editor ->
      addNestedScopes(editor)
      typeText(editor, "${jumpFrom}G")
      typeText(editor, "330G")
      assertCentred(screenState(editor))
      moveAndCheck(editor, motion, count = 5)
    }
  }

  @Test
  fun `test move down from top of file only scrolls once caret passes the middle`() {
    withCentredCaret { editor ->
      addNestedScopes(editor)
      typeText(editor, "gg")
      assertDoesNotScroll(editor, List(screenHeight / 2) { "j" })
      moveAndCheck(editor, "j", count = 20)
    }
  }

  @Test
  fun `test move up from bottom of file only scrolls once caret passes the middle`() {
    withCentredCaret { editor ->
      addNestedScopes(editor)
      typeText(editor, "G")
      assertDoesNotScroll(editor, List(screenHeight / 2) { "k" })
      moveAndCheck(editor, "k", count = 20)
    }
  }

  @Test
  fun `test changing lines does not scroll with sticky lines`() {
    withCentredCaret { editor ->
      addNestedScopes(editor)
      typeText(editor, "330G")
      assertDoesNotScroll(editor, listOf("cc", "a", "b", "<Esc>", "S", "c", "<Esc>", "3cc", "d", "<Esc>"))
    }
  }

  // Opening a line moves the ends of the scopes around the caret, which can give an exact middle where there wasn't one
  // before. Scrolling there once is fine, scrolling back and forth isn't
  @Test
  fun `test opening line above keeps caret centred and does not scroll while typing with sticky lines`() {
    withCentredCaret { editor ->
      addNestedScopes(editor)
      typeText(editor, "330G")
      typeText(editor, "O")
      assertCentred(screenState(editor))
      assertScrollingCaretIntoViewAgainDoesNotScroll(editor, "After 'O'")
      assertDoesNotScroll(editor, listOf("a", "b", "<Esc>"))
    }
  }

  private data class ScreenState(
    val caretLine: Int,
    val scrollOffset: Int,
    val stickyLines: Int,
    val rowsAboveCaret: Int,
    val rowsBelowCaret: Int,
  ) {
    val isCaretVisible: Boolean
      get() = rowsAboveCaret >= 0 && rowsBelowCaret >= 0

    // With an even number of rows, the middle is the upper of the two middle rows
    val rowsOffCentre: Int
      get() = abs(rowsAboveCaret - (rowsAboveCaret + rowsBelowCaret) / 2)
  }

  /**
   * Moves the caret [count] times, and checks after every move that:
   * - the editor didn't scroll against the move, and if it didn't scroll at all, the caret is now exactly in the middle
   * - the editor scrolled [linesPerMove] lines, if given
   * - the caret is visible, and at most [maxRowsOffCentre] rows off the middle, if given
   * - scrolling the caret into view again doesn't scroll, otherwise the editor jumps while typing
   */
  private fun moveAndCheck(
    editor: Editor,
    motion: String,
    count: Int = 30,
    maxRowsOffCentre: Int? = 1,
    linesPerMove: Int? = null,
  ) {
    val direction = if (motion == "j") 1 else -1
    repeat(count) { move ->
      val before = screenState(editor)
      typeText(editor, motion)
      val after = screenState(editor)
      val scrolled = (after.scrollOffset - before.scrollOffset) * direction
      val message = "'$motion' #$move: $before -> $after"

      assertEquals(before.caretLine + direction, after.caretLine, "Caret didn't move. $message")
      assertTrue(scrolled >= 0, "Editor scrolled against the move. $message")
      if (scrolled == 0) {
        assertCentred(after, maxRowsOffCentre = 0, "Editor didn't scroll. $message")
      }
      if (linesPerMove != null) {
        assertEquals(linesPerMove * editor.lineHeight, scrolled, "Editor scrolled the wrong number of lines. $message")
      }
      assertTrue(after.isCaretVisible, "Caret isn't visible. $message")
      if (maxRowsOffCentre != null) {
        assertCentred(after, maxRowsOffCentre, message)
      }
      assertScrollingCaretIntoViewAgainDoesNotScroll(editor, message)
    }
  }

  private fun assertCentred(state: ScreenState, maxRowsOffCentre: Int = 1, message: String = "") {
    assertTrue(state.rowsOffCentre <= maxRowsOffCentre, "Caret is ${state.rowsOffCentre} rows off centre. $message")
  }

  private fun assertScrollingCaretIntoViewAgainDoesNotScroll(editor: Editor, message: String) {
    val scrollOffset = verticalScrollOffset(editor)
    ApplicationManager.getApplication().invokeAndWait { ScrollViewHelper.scrollCaretIntoView(editor) }
    assertEquals(scrollOffset, verticalScrollOffset(editor), "Scrolling the caret into view again scrolled. $message")
  }

  private fun assertDoesNotScroll(editor: Editor, keys: List<String>) {
    val scrollOffset = verticalScrollOffset(editor)
    keys.forEachIndexed { index, key ->
      typeText(editor, key)
      assertEquals(scrollOffset, verticalScrollOffset(editor), "Editor scrolled after '$key' #$index")
    }
  }

  private fun screenState(editor: Editor): ScreenState {
    var state: ScreenState? = null
    ApplicationManager.getApplication().invokeAndWait {
      val visibleArea = EditorHelper.getVisibleArea(editor)
      val caretY = editor.visualLineToY(editor.caretModel.visualPosition.line)
      state = ScreenState(
        caretLine = editor.caretModel.logicalPosition.line,
        scrollOffset = editor.scrollingModel.verticalScrollOffset,
        stickyLines = (editor as EditorImpl).stickyLinesPanelHeight / editor.lineHeight,
        rowsAboveCaret = (caretY - visibleArea.y) / editor.lineHeight,
        rowsBelowCaret = (visibleArea.y + visibleArea.height - caretY - editor.lineHeight) / editor.lineHeight,
      )
    }
    return state!!
  }

  // The sticky lines panel is only created for main editors, the fixture's editor doesn't have one
  private fun withCentredCaret(action: (Editor) -> Unit) {
    configureByLines(1000, "    I found it in a legendary land")
    enterCommand("set scrolloff=999")

    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
      editor = EditorFactory.getInstance()
        .createEditor(fixture.editor.document, fixture.project, EditorKind.MAIN_EDITOR)
      EditorTestUtil.setEditorVisibleSizeInPixels(
        editor,
        (screenWidth * EditorHelper.getPlainSpaceWidthFloat(editor!!)).roundToInt(),
        screenHeight * editor.lineHeight,
      )
    }
    try {
      action(editor!!)
    } finally {
      ApplicationManager.getApplication().invokeAndWait { EditorFactory.getInstance().releaseEditor(editor!!) }
    }
  }

  private fun addStickyLine(editor: Editor, primaryLine: Int, scopeLine: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val document = editor.document
      val model = StickyLinesModel.getModel(fixture.project, document)!!
      model.addStickyLine(document.getLineStartOffset(primaryLine), document.getLineEndOffset(scopeLine), null)
      model.notifyLinesUpdate()
    }
  }

  // A class every 40 lines, with a method every 10 lines and a nested block in the first method
  private fun addNestedScopes(editor: Editor) {
    for (classLine in 0 until 960 step 40) {
      addStickyLine(editor, classLine, classLine + 39)
      for (methodLine in classLine + 2 until classLine + 38 step 10) {
        addStickyLine(editor, methodLine, methodLine + 8)
      }
      addStickyLine(editor, classLine + 3, classLine + 9)
    }
  }

  // A rendered doc comment above every method from addNestedScopes
  private fun addDocComments(editor: Editor) {
    for (classLine in 0 until 960 step 40) {
      for (methodLine in classLine + 2 until classLine + 38 step 10) {
        addInlayAbove(editor, methodLine, 4)
      }
    }
  }

  private fun addInlayAbove(editor: Editor, line: Int, heightInRows: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val widthInPixels = (EditorHelper.getPlainSpaceWidthFloat(editor) * 10).roundToInt()
      val offset = editor.document.getLineStartOffset(line)
      EditorTestUtil.addBlockInlay(editor, offset, false, true, widthInPixels, heightInRows * editor.lineHeight)
    }
  }

  private fun typeText(editor: Editor, keys: String) =
    typeText(injector.parser.parseKeys(keys), editor, fixture.project)

  private fun verticalScrollOffset(editor: Editor): Int {
    var offset = 0
    ApplicationManager.getApplication().invokeAndWait { offset = editor.scrollingModel.verticalScrollOffset }
    return offset
  }
}

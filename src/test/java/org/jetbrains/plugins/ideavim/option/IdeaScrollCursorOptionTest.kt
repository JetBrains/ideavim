/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.util.ui.UIUtil
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.event.MouseWheelEvent
import kotlin.test.assertEquals

/**
 * In Vim, scrolling with the mouse wheel moves the cursor along with the text, so it never leaves the window (and
 * respects 'scrolloff'). IdeaVim does this only when 'ideascrollcursor' is set, to keep the default IntelliJ behaviour.
 *
 * One mouse wheel notch scrolls three lines, the same as Vim's default 'mousescroll'.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class IdeaScrollCursorOptionTest : VimTestCase() {
  @Test
  fun `test mouse wheel does not move caret by default`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    scrollWithMouseWheel(12)
    assertTopLogicalLine(36)
    assertPosition(0, 0)
  }

  @Test
  fun `test mouse wheel does not move caret when option is reset`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set noideascrollcursor")
    setPositionAndScroll(0, 0)
    scrollWithMouseWheel(12)
    assertTopLogicalLine(36)
    assertPosition(0, 0)
  }

  @Test
  fun `test mouse wheel down moves caret to top of screen`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 0)
    scrollWithMouseWheel(12)
    assertVisibleArea(36, 70)
    assertPosition(36, 0)
  }

  @Test
  fun `test mouse wheel up moves caret to bottom of screen`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(100, 100)
    scrollWithMouseWheel(-12)
    assertVisibleArea(64, 98)
    assertPosition(98, 0)
  }

  @Test
  fun `test mouse wheel does not move caret that is still on screen`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 20)
    scrollWithMouseWheel(1)
    assertTopLogicalLine(3)
    assertPosition(20, 0)
  }

  @Test
  fun `test caret follows each mouse wheel notch`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 0)
    scrollWithMouseWheel(1)
    assertPosition(3, 0)
    scrollWithMouseWheel(1)
    assertPosition(6, 0)
  }

  @Test
  fun `test mouse wheel down respects scrolloff`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 5)
    scrollWithMouseWheel(12)
    assertTopLogicalLine(36)
    assertPosition(41, 0)
  }

  @Test
  fun `test mouse wheel up respects scrolloff`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(100, 105)
    scrollWithMouseWheel(-12)
    assertVisibleArea(64, 98)
    assertPosition(93, 0)
  }

  @Test
  fun `test mouse wheel keeps caret column`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 0, 10)
    scrollWithMouseWheel(12)
    assertPosition(36, 10)
  }

  @Test
  fun `test mouse wheel keeps desired column after end of line`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 0)
    typeText("$")
    scrollWithMouseWheel(12)
    // Every line is "Lorem ipsum dolor sit amet,", so "$" stays on the last character
    assertPosition(36, 26)
  }

  @Test
  fun `test mouse wheel extends selection in Visual mode`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 0)
    typeText("v")
    scrollWithMouseWheel(12)
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertPosition(36, 0)
    ApplicationManager.getApplication().runReadAction {
      val editor = fixture.editor
      assertEquals(0, editor.selectionModel.selectionStart)
      assertEquals(editor.document.getLineStartOffset(36) + 1, editor.selectionModel.selectionEnd)
    }
  }

  @Test
  fun `test mouse wheel moves caret in Insert mode`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 0)
    typeText("i")
    scrollWithMouseWheel(12)
    assertMode(Mode.INSERT)
    assertPosition(36, 0)
  }

  @Test
  fun `test dragging scrollbar moves caret`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 0)
    dragVerticalScrollBarTo(36)
    assertTopLogicalLine(36)
    assertPosition(36, 0)
  }

  @Test
  fun `test programmatic scroll does not move caret`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 0)
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.scrollingModel.scrollVertically(36 * fixture.editor.lineHeight)
    }
    assertTopLogicalLine(36)
    assertPosition(0, 0)
  }

  @Test
  fun `test vim scroll commands are unaffected`() {
    configureByPages(5)
    enterCommand("set ideascrollcursor")
    enterCommand("set scrolloff=0")
    setPositionAndScroll(0, 20)
    typeText("<C-E>")
    assertVisibleArea(1, 35)
    assertPosition(20, 0)
  }

  /**
   * Dispatches real mouse wheel events to the editor's scroll pane. Positive [notches] scroll down, negative up
   */
  private fun scrollWithMouseWheel(notches: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val scrollPane = (fixture.editor as EditorEx).scrollPane
      val rotation = if (notches > 0) 1 else -1
      repeat(kotlin.math.abs(notches)) {
        val event = MouseWheelEvent(
          scrollPane,
          MouseWheelEvent.MOUSE_WHEEL,
          System.currentTimeMillis(),
          0,
          10,
          10,
          0,
          false,
          MouseWheelEvent.WHEEL_UNIT_SCROLL,
          3,
          rotation,
        )
        scrollPane.dispatchEvent(event)
        UIUtil.dispatchAllInvocationEvents()
      }
    }
  }

  /**
   * Mimics dragging the scrollbar thumb, which marks the scrollbar model as adjusting while the value changes
   */
  private fun dragVerticalScrollBarTo(topLogicalLine: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val scrollBar = (fixture.editor as EditorEx).scrollPane.verticalScrollBar
      scrollBar.valueIsAdjusting = true
      scrollBar.value = topLogicalLine * fixture.editor.lineHeight
      scrollBar.valueIsAdjusting = false
      UIUtil.dispatchAllInvocationEvents()
    }
  }
}

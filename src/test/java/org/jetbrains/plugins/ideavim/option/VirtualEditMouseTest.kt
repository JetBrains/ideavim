/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.VisualPosition
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssertMode
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import kotlin.test.assertEquals

/**
 * Mouse clicks and drags after the end of a line. Without 'virtualedit' the results are the same as before it was
 * supported, also when the IDE allows placing the caret after the end of line
 */
@TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
class VirtualEditMouseTest : VimTestCase() {
  private val text = "the\nhouse\n\nok"

  private fun setUpWith(ve: String?, ideVirtualSpace: Boolean = false) {
    configureByText(text)
    if (ideVirtualSpace) {
      ApplicationManager.getApplication().invokeAndWait { fixture.editor.settings.isVirtualSpace = true }
    }
    if (ve != null) enterCommand("set virtualedit=$ve")
  }

  private fun dispatchMouseEvent(id: Int, line: Int, column: Int, modifiers: Int) {
    val editor = fixture.editor
    val point = editor.visualPositionToXY(VisualPosition(line, column))
    val event = MouseEvent(
      editor.contentComponent, id, System.currentTimeMillis(), modifiers, point.x + 1, point.y + 1, 1, false,
      MouseEvent.BUTTON1,
    )
    editor.contentComponent.dispatchEvent(event)
  }

  private fun click(line: Int, column: Int) = ApplicationManager.getApplication().invokeAndWait {
    dispatchMouseEvent(MouseEvent.MOUSE_PRESSED, line, column, InputEvent.BUTTON1_DOWN_MASK)
    dispatchMouseEvent(MouseEvent.MOUSE_RELEASED, line, column, 0)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
  }

  private fun drag(fromLine: Int, fromColumn: Int, toLine: Int, toColumn: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      dispatchMouseEvent(MouseEvent.MOUSE_PRESSED, fromLine, fromColumn, InputEvent.BUTTON1_DOWN_MASK)
      // The first drag events are taken as part of a click
      repeat(6) { dispatchMouseEvent(MouseEvent.MOUSE_DRAGGED, toLine, toColumn, InputEvent.BUTTON1_DOWN_MASK) }
      dispatchMouseEvent(MouseEvent.MOUSE_RELEASED, toLine, toColumn, 0)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }

  private fun assertSelection(start: Int, end: Int) {
    ApplicationManager.getApplication().runReadAction {
      assertEquals(start to end, fixture.editor.selectionModel.selectionStart to fixture.editor.selectionModel.selectionEnd)
    }
  }

  @Test
  fun `test click after line end with ve=all`() {
    setUpWith("all")
    click(0, 8)
    assertPosition(0, 8)
  }

  @Test
  fun `test insert after click after line end with ve=all pads with spaces`() {
    setUpWith("all")
    click(0, 8)
    typeText("iX<Esc>")
    assertEquals("the     X\nhouse\n\nok", fixture.editor.document.text)
  }

  @Test
  fun `test drag to after line end with ve=all`() {
    setUpWith("all")
    drag(1, 3, 0, 8)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertPosition(0, 8)
    assertSelection(3, 7)
  }

  @Test
  fun `test click after line end in Normal mode with ve=insert`() {
    setUpWith("insert")
    click(0, 8)
    assertPosition(0, 2)
  }

  @Test
  fun `test click after line end in Insert mode with ve=insert pads with spaces`() {
    setUpWith("insert")
    typeText("i")
    click(0, 8)
    typeText("X<Esc>")
    assertEquals("the     X\nhouse\n\nok", fixture.editor.document.text)
  }

  @Test
  fun `test drag to after line end in Insert mode with ve=insert keeps selection`() {
    setUpWith("insert")
    typeText("i")
    drag(1, 3, 0, 8)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT))
    assertSelection(3, 7)
  }

  @Test
  fun `test click after line end with IDE virtual space`() {
    setUpWith(null, ideVirtualSpace = true)
    click(0, 8)
    assertPosition(0, 2)
  }

  @Test
  fun `test insert after click after line end with ve=onemore and IDE virtual space`() {
    setUpWith("onemore", ideVirtualSpace = true)
    click(0, 8)
    typeText("iX<Esc>")
    assertEquals("theX\nhouse\n\nok", fixture.editor.document.text)
  }

  @Test
  fun `test drag to after line end with IDE virtual space`() {
    setUpWith(null, ideVirtualSpace = true)
    drag(1, 3, 0, 8)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertPosition(0, 3)
    assertSelection(3, 7)
  }

  @Test
  fun `test escape after drag to after line end with ve=onemore and IDE virtual space`() {
    setUpWith("onemore", ideVirtualSpace = true)
    drag(1, 3, 0, 8)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    typeText("<Esc>")
    assertPosition(0, 2)
  }

  @Test
  fun `test escape after drag to after line end in Insert mode`() {
    setUpWith(null)
    typeText("i")
    drag(1, 3, 0, 8)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.INSERT))
    typeText("<Esc>")
    assertPosition(0, 2)
  }

  @Test
  fun `test escape after Visual mode click after line end with IDE virtual space`() {
    setUpWith(null, ideVirtualSpace = true)
    typeText("v")
    click(0, 8)
    typeText("<Esc>")
    assertPosition(0, 2)
  }

  @Test
  fun `test escape after Visual mode click after line end with ve=onemore and IDE virtual space`() {
    setUpWith("onemore", ideVirtualSpace = true)
    typeText("v")
    click(0, 8)
    typeText("<Esc>")
    assertPosition(0, 3)
  }
}

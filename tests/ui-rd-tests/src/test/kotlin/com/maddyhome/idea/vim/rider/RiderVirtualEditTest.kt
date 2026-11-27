/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.awt.event.KeyEvent

/**
 * 'virtualedit' in Rider (VIM-1193). The editor's virtual space setting and the spaces the editor inserts to fill
 * virtual space when typing after the line end go through the ReSharper backend, and in split mode through the
 * protocol between the frontend and the backend.
 */
abstract class RiderVirtualEditTestBase : IdeaVimRiderTestBase() {

  @AfterEach
  fun resetVirtualEdit() {
    esc()
    exCommand("set virtualedit=")
  }

  private fun virtualEdit(value: String) {
    exCommand("set virtualedit=$value")
  }

  private fun assertCaretColumn(expected: Int) {
    var column = -1
    waitUntil { column = caretColumn(); column == expected }
    assertEquals(expected, column, "Caret column")
  }

  /** Asserts that the text stays [expected] for a while, for commands that must not change it */
  private fun assertTextStays(expected: String) {
    assertText(expected)
    Thread.sleep(1500)
    assertEquals(expected, editorText())
  }

  @Test
  fun `insert in virtual space pads with spaces`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("$5l")
    assertCaretColumn(12)
    typeVim("ix")
    esc()
    assertText("line one    x\nsecond line here\nab")
  }

  @Test
  fun `append in virtual space pads with spaces`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("$5la!")
    esc()
    assertText("line one     !\nsecond line here\nab")
  }

  @Test
  fun `replace character in virtual space pads with spaces`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("G5lrZ")
    assertText("line one\nsecond line here\nab   Z")
  }

  @Test
  fun `put in virtual space pads with spaces`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("yiw$5lp")
    assertText("line one     line\nsecond line here\nab")
  }

  @Test
  fun `append with count in virtual space pads once`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("$5l3a!")
    esc()
    assertText("line one     !!!\nsecond line here\nab")
  }

  @Test
  fun `repeat insert in virtual space on another line pads again`() {
    setText("ab\ncd\nef")
    virtualEdit("all")
    typeVim("5lix")
    esc()
    assertText("ab   x\ncd\nef")
    typeVim("j.")
    assertText("ab   x\ncd   x\nef")
  }

  @Test
  fun `backspace in virtual space is not repeated`() {
    setText("alpha bravo\nab\n\nlonger line here")
    virtualEdit("all")
    typeVim("j6li")
    key(KeyEvent.VK_BACK_SPACE)
    typeVim("X")
    esc()
    assertText("alpha bravo\nab   X\n\nlonger line here")
    typeVim("jj6l.")
    assertText("alpha bravo\nab   X\n\nlonger lineX here")
  }

  @Test
  fun `delete word in virtual space is not repeated`() {
    setText("alpha bravo\nab\n\nlonger line here")
    virtualEdit("all")
    typeVim("j6li")
    ctrl(KeyEvent.VK_W)
    typeVim("X")
    esc()
    assertText("alpha bravo\nabX\n\nlonger line here")
    typeVim("jj6l.")
    assertText("alpha bravo\nabX\n\nlonger lXine here")
  }

  @Test
  fun `undo padded insert in one step`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("$5lix")
    esc()
    assertText("line one    x\nsecond line here\nab")
    typeVim("u")
    assertText(TEXT)
  }

  @Test
  fun `x in virtual space does nothing`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("$5lx")
    assertTextStays(TEXT)
    assertCaretColumn(12)
  }

  @Test
  fun `D in virtual space does nothing`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("$5lD")
    assertTextStays(TEXT)
    assertCaretColumn(12)
  }

  @Test
  fun `arrow right in insert mode moves past line end with ve=insert`() {
    setText(TEXT)
    virtualEdit("insert")
    typeVim("A")
    key(KeyEvent.VK_RIGHT)
    key(KeyEvent.VK_RIGHT)
    typeVim("y")
    esc()
    assertText("line one  y\nsecond line here\nab")
  }

  @Test
  fun `leaving insert mode with ve=insert returns caret to text`() {
    setText(TEXT)
    virtualEdit("insert")
    assertFalse(isVirtualSpace(), "virtual space in Normal mode")
    typeVim("A")
    key(KeyEvent.VK_RIGHT)
    key(KeyEvent.VK_RIGHT)
    assertCaretColumn(10)
    assertTrue(isVirtualSpace(), "virtual space in Insert mode")
    esc()
    assertCaretColumn(7)
    assertEquals(7, caretOffset())
    assertFalse(isVirtualSpace(), "virtual space after Insert mode")
  }

  @Test
  fun `expression register at line end with ve=insert`() {
    setText(TEXT)
    virtualEdit("insert")
    typeVim("A")
    ctrl(KeyEvent.VK_R)
    typeVim("=1")
    enter()
    esc()
    assertText("line one1\nsecond line here\nab")
  }

  @Test
  fun `resetting ve moves caret back into text and stops padding`() {
    setText(TEXT)
    virtualEdit("all")
    typeVim("$5l")
    assertCaretColumn(12)
    virtualEdit("")
    assertCaretColumn(7)
    assertFalse(isVirtualSpace(), "virtual space after resetting 've'")
    typeVim("Ax")
    esc()
    assertText("line onex\nsecond line here\nab")
  }

  @Test
  fun `append and repeat without ve`() {
    setText(TEXT)
    typeVim("Abc")
    esc()
    typeVim("j.")
    assertText("line onebc\nsecond line herebc\nab")
  }

  @Test
  fun `x at line end without ve`() {
    setText(TEXT)
    typeVim("\$x")
    assertText("line on\nsecond line here\nab")
    assertEquals(6, caretOffset())
  }

  @Test
  fun `put without ve`() {
    setText(TEXT)
    typeVim("yiwjp")
    assertText("line one\nslineecond line here\nab")
  }

  companion object {
    private const val TEXT = "line one\nsecond line here\nab"
  }
}

class RiderVirtualEditTest : RiderVirtualEditTestBase()

@EnabledOnOs(OS.LINUX)
class RiderSplitVirtualEditTest : RiderVirtualEditTestBase() {
  override val splitMode: Boolean = true
}

/**
 * Diagnostic, not a regression test: prints the state of the backend's editors while the frontend caret is in virtual
 * space, to see where the fill of typed text gets lost
 */
@EnabledOnOs(OS.LINUX)
class RiderSplitBackendVirtualSpaceTest : IdeaVimRiderTestBase() {
  override val splitMode: Boolean = true

  @Test
  fun `print backend editor state in virtual space`() {
    setText("line one\nsecond line here\nab")
    exCommand("set virtualedit=all")
    Thread.sleep(1000)
    println("PROBE after :set ve=all: ${backendEditorStates()} frontendVirtualSpace=${isVirtualSpace()}")
    typeVim("$5l")
    Thread.sleep(1500)
    println("PROBE after \$5l: ${backendEditorStates()} frontend column=${caretColumn()} offset=${caretOffset()}")
    typeVim("i")
    Thread.sleep(1500)
    println("PROBE after i: ${backendEditorStates()} frontend column=${caretColumn()}")
    typeVim("x")
    Thread.sleep(2000)
    println("PROBE after x: ${backendEditorStates()} frontend column=${caretColumn()} text=${editorText().lineSequence().first()}|")
  }
}

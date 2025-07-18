/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.helper

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.keyStroke
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import javax.swing.KeyStroke
import kotlin.test.assertEquals

class StringHelperTest : VimTestCase() {
  @Test
  fun testFalseSpecialKey() {
    val expectedKeys = injector.parser.parseKeys("move '<-2").toMutableList()
    expectedKeys.addAll(injector.parser.parseKeys("<CR>"))
    assertEquals(expectedKeys, injector.parser.parseKeys("move '<-2<CR>"))
  }

  @Test
  fun testParseKeyModifiers() {
    assertTypedKeyStroke('C', "C")
    assertTypedKeyStroke('c', "c")
    assertPressedKeyStroke("control C", "<C-C>")
    assertPressedKeyStroke("control C", "<C-c>")
    assertPressedKeyStroke("control C", "<c-c>")
    assertPressedKeyStroke("alt C", "<A-C>")
    assertPressedKeyStroke("alt C", "<a-c>")
    assertPressedKeyStroke("alt C", "<M-C>")
    assertPressedKeyStroke("meta C", "<D-C>")
    assertPressedKeyStroke("control shift C", "<C-S-C>")
    assertPressedKeyStroke("alt shift C", "<A-S-C>")
    assertPressedKeyStroke("control alt C", "<C-A-C>")
    assertPressedKeyStroke("control alt shift C", "<C-A-S-C>")
    assertPressedKeyStroke("control alt shift C", "<C-A-S-C>")
    assertPressedKeyStroke("meta control alt shift C", "<D-C-A-S-C>")
  }

  @Test
  fun testParseSpecialKeys() {
    assertTypedKeyStroke('<', "<")
    assertTypedKeyStroke('>', ">")
    assertTypedKeyStroke('\\', "\\")
    assertTypedKeyStroke('\\', "<Leader>")
    assertPressedKeyStroke("ESCAPE", "<Esc>")
    assertPressedKeyStroke("ENTER", "<Enter>")
    assertPressedKeyStroke("ENTER", "<Return>")
    assertPressedKeyStroke("ENTER", "<CR>")
  }

  // VIM-645
  @Test
  fun testParseSpaceAsTyped() {
    assertTypedKeyStroke(' ', "<Space>")
  }

  // VIM-660
  @Test
  fun testParseCtrlSpace() {
    assertPressedKeyStroke("control SPACE", "<C-Space>")
  }

  // VIM-655
  @Test
  fun testParseTypedShiftChar() {
    assertTypedKeyStroke('H', "<S-h>")
  }

  // VIM-651
  @Test
  fun testParseBackspace() {
    assertPressedKeyStroke("BACK_SPACE", "<BS>")
    assertPressedKeyStroke("BACK_SPACE", "<Backspace>")
  }

  // VIM-666
  @Test
  fun testParseBarSpecialKey() {
    assertTypedKeyStroke('|', "<Bar>")
  }

  // VIM-679
  @Test
  fun testControlXCharacter() {
    assertPressedKeyStroke("control X", "\u0018")
  }

  @Test
  fun testControlBoundCharacters() {
    assertKeyStroke(KeyStroke.getKeyStroke('@'.code, InputEvent.CTRL_DOWN_MASK), "\u0000")
    assertKeyStroke(KeyStroke.getKeyStroke('_'.code, InputEvent.CTRL_DOWN_MASK), "\u001F")
  }

  @Test
  fun testControlExceptionCharacters() {
    assertPressedKeyStroke("TAB", "\t") // U+0009
    assertPressedKeyStroke("ENTER", "\n") // U+000A
  }

  private fun assertPressedKeyStroke(expected: String, actual: String) {
    assertEquals(KeyStroke.getKeyStroke(expected), parseKeyStroke(actual))
  }

  private fun assertKeyStroke(expected: KeyStroke, actual: String) {
    assertEquals(expected, parseKeyStroke(actual))
  }

  private fun assertTypedKeyStroke(expected: Char, actual: String) {
    assertEquals(KeyStroke.getKeyStroke(expected), parseKeyStroke(actual))
  }

  private fun parseKeyStroke(s: String): KeyStroke {
    val actualStrokes = injector.parser.parseKeys(s)
    assertEquals<Any>(1, actualStrokes.size, injector.parser.toKeyNotation(actualStrokes))
    return actualStrokes[0].keyStroke
  }
}

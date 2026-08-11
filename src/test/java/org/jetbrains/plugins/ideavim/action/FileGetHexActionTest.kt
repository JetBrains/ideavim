/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for `g8`, which prints the hex values of the UTF-8 bytes of the character under the caret.
 */
class FileGetHexActionTest : VimTestCase() {
  @Test
  fun `test get hex of ascii character`() {
    configureByText("${c}abc")
    typeText("g8")
    assertEquals("61", VimPlugin.getMessage())
  }

  @Test
  fun `test get hex of ascii character in the middle of the line`() {
    configureByText("ab${c}c")
    typeText("g8")
    assertEquals("63", VimPlugin.getMessage())
  }

  @Test
  fun `test get hex of two byte character`() {
    configureByText("${c}é")
    typeText("g8")
    assertEquals("c3 a9", VimPlugin.getMessage())
  }

  @Test
  fun `test get hex of three byte character`() {
    configureByText("${c}€")
    typeText("g8")
    assertEquals("e2 82 ac", VimPlugin.getMessage())
  }

  @Test
  fun `test get hex of four byte character outside the BMP`() {
    // Stored as a surrogate pair. Vim reports all four UTF-8 bytes, not the value of a single UTF-16 code unit
    configureByText("$c😀")
    typeText("g8")
    assertEquals("f0 9f 98 80", VimPlugin.getMessage())
  }

  @Test
  fun `test get hex of tab pads the value to two digits`() {
    configureByText("${c}\tabc")
    typeText("g8")
    assertEquals("09", VimPlugin.getMessage())
  }

  @Test
  fun `test get hex of NUL byte in the buffer`() {
    // Vim stores a NUL inside a line as a line feed and maps it back when reporting, so it prints "00" rather than
    // treating the character as a line break
    configureByText("a${c}\u0000b")
    typeText("g8")
    assertEquals("00", VimPlugin.getMessage())
  }

  // Vim's cursor cannot sit on a line break - at the end of a line it sits on the line's terminating NUL. There is no
  // character under the caret, so Vim reports "NUL" rather than the value of the line break character.
  @Test
  fun `test get hex on empty file reports NUL`() {
    configureByText("")
    typeText("g8")
    assertEquals("NUL", VimPlugin.getMessage())
  }

  @Test
  fun `test get hex on empty line reports NUL`() {
    val before = """
      |Lorem ipsum
      |$c
      |dolor sit amet
    """.trimMargin()
    configureByText(before)
    typeText("g8")
    assertEquals("NUL", VimPlugin.getMessage())
  }

  @Test
  fun `test get hex on empty last line reports NUL`() {
    val before = """
      |Lorem ipsum
      |$c
    """.trimMargin()
    configureByText(before)
    typeText("g8")
    assertEquals("NUL", VimPlugin.getMessage())
  }
}

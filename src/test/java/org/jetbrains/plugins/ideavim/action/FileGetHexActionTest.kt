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

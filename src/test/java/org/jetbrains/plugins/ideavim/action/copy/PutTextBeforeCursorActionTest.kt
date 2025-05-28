/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
import org.junit.jupiter.api.Test

class PutTextBeforeCursorActionTest : VimTestCase() {
  /**
   * @author Oskar Persson
   */
  @Test
  fun `test put visual text character to line twice with separate commands large P`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    ApplicationManager.getApplication().runReadAction {
      registerService.storeText(
        vimEditor,
        context,
        vimEditor.primaryCaret(),
        before rangeOf "Discovery",
        SelectionType.CHARACTER_WISE,
        false
      )
    }
    typeText(injector.parser.parseKeys("V" + "P"))
    typeText(injector.parser.parseKeys("V" + "P"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test undo after put before cursor`() {
    configureByText("Hello ${c}world")
    typeText("yy")
    typeText("P")
    assertState(
      """
      ${c}Hello world
      Hello world
    """.trimIndent()
    )
    typeText("u")
    assertState("Hello ${c}world")
  }

  @Test
  fun `test undo after put character before cursor`() {
    configureByText("abc${c}def")
    typeText("yl")  // Yank 'd'
    typeText("h")   // Move left
    assertState("ab${c}cdef")
    typeText("P")
    assertState("ab${c}dcdef")
    typeText("u")
    assertState("ab${c}cdef")
  }

  @Test
  fun `test undo after put word before cursor`() {
    configureByText("The ${c}quick brown fox")
    typeText("yiw")  // Yank "quick"
    typeText("w")    // Move to "brown"
    assertState("The quick ${c}brown fox")
    typeText("P")
    assertState("The quick quic${c}kbrown fox")
    typeText("u")
    assertState("The quick ${c}brown fox")
  }

  @Test
  fun `test multiple undo after sequential puts`() {
    configureByText("${c}Hello")
    typeText("yy")
    typeText("P")
    assertState(
      """
      ${c}Hello
      Hello
    """.trimIndent()
    )
    typeText("P")
    assertState(
      """
      ${c}Hello
      Hello
      Hello
    """.trimIndent()
    )

    // Undo second put
    typeText("u")
    assertState(
      """
      ${c}Hello
      Hello
    """.trimIndent()
    )

    // Undo first put
    typeText("u")
    assertState(
      """
      ${c}Hello
    """.trimIndent()
    )
  }

  @Test
  fun `test undo put visual block`() {
    configureByText(
      """
      ${c}abc
      def
      ghi
    """.trimIndent()
    )
    typeText("<C-V>jjl")  // Visual block select first 2 columns of all lines
    typeText("y")
    typeText("$")
    assertState(
      """
      ab${c}c
      def
      ghi
    """.trimIndent()
    )
    typeText("P")
    assertState(
      """
      ab${c}abc
      dedef
      ghghi
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      ab${c}c
      def
      ghi
    """.trimIndent()
    )
  }

  @Test
  fun `test undo after put before cursor with oldundo`() {
    configureByText("Hello ${c}world")
    try {
      enterCommand("set oldundo")
      typeText("yy")
      typeText("P")
      assertState(
        """
      ${c}Hello world
      Hello world
    """.trimIndent()
      )
      typeText("u")
      assertState("Hello ${c}world")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after put character before cursor with oldundo`() {
    configureByText("abc${c}def")
    try {
      enterCommand("set oldundo")
      typeText("yl")  // Yank 'd'
      typeText("h")   // Move left
      assertState("ab${c}cdef")
      typeText("P")
      assertState("ab${c}dcdef")
      typeText("u")
      assertState("ab${c}cdef")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after put word before cursor with oldundo`() {
    configureByText("The ${c}quick brown fox")
    try {
      enterCommand("set oldundo")
      typeText("yiw")  // Yank "quick"
      typeText("w")    // Move to "brown"
      assertState("The quick ${c}brown fox")
      typeText("P")
      assertState("The quick quic${c}kbrown fox")
      typeText("u")
      assertState("The quick ${c}brown fox")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test multiple undo after sequential puts with oldundo`() {
    configureByText("${c}Hello")
    try {
      enterCommand("set oldundo")
      typeText("yy")
      typeText("P")
      assertState(
        """
      ${c}Hello
      Hello
    """.trimIndent()
      )
      typeText("P")
      assertState(
        """
      ${c}Hello
      Hello
      Hello
    """.trimIndent()
      )

      // Undo second put
      typeText("u")
      assertState(
        """
      ${c}Hello
      Hello
    """.trimIndent()
      )

      // Undo first put
      typeText("u")
      assertState(
        """
      ${c}Hello
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo put visual block with oldundo`() {
    configureByText(
      """
      ${c}abc
      def
      ghi
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("<C-V>jjl")  // Visual block select first 2 columns of all lines
      typeText("y")
      typeText("$")
      assertState(
        """
      ab${c}c
      def
      ghi
    """.trimIndent()
      )
      typeText("P")
      assertState(
        """
      ab${c}abc
      dedef
      ghghi
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      ab${c}c
      def
      ghi
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }
}

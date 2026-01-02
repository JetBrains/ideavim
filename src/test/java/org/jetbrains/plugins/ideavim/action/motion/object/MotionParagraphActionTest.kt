/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for paragraph text objects (ip, ap) which use FLAG_TEXT_BLOCK.
 *
 * FLAG_TEXT_BLOCK affects visual mode behavior:
 * - Selection anchor is reset to block start when applying text object
 * - Entire block is selected regardless of selection direction
 */
@Suppress("SpellCheckingInspection")
class MotionParagraphActionTest : VimTestCase() {

  // ============== Inner Paragraph (ip) ==============

  @Test
  fun `test inner paragraph from middle of paragraph`() {
    doTest(
      "vip",
      """
        |First paragraph line one.
        |First paragraph line two.
        |
        |Second para${c}graph line one.
        |Second paragraph line two.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph line one.
        |First paragraph line two.
        |
        |${s}Second paragraph line one.
        |${c}Second paragraph line two.
        |${se}
        |Third paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test inner paragraph from start of paragraph`() {
    doTest(
      "vip",
      """
        |First paragraph line one.
        |First paragraph line two.
        |
        |${c}Second paragraph line one.
        |Second paragraph line two.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph line one.
        |First paragraph line two.
        |
        |${s}Second paragraph line one.
        |${c}Second paragraph line two.
        |${se}
        |Third paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test inner paragraph on empty line selects empty lines`() {
    doTest(
      "vip",
      """
        |First paragraph.
        |${c}
        |
        |Second paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |${s}
        |${c}
        |${se}Second paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test inner paragraph with backwards selection`() {
    // Start visual mode, move backwards (to empty line), then apply ip
    // Current behavior: selects the empty line block
    doTest(
      listOf("v", "k", "ip"),
      """
        |First paragraph.
        |
        |Second para${c}graph line one.
        |Second paragraph line two.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |${s}${c}
        |${se}Second paragraph line one.
        |Second paragraph line two.
        |
        |Third paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test inner paragraph with backwards selection crossing paragraph boundary`() {
    // Move cursor back into previous paragraph
    doTest(
      listOf("v", "k", "k", "k", "ip"),
      """
        |First paragraph.
        |
        |Second para${c}graph line one.
        |Second paragraph line two.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |${s}${c}First paragraph.
        |${se}
        |Second paragraph line one.
        |Second paragraph line two.
        |
        |Third paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test inner paragraph on first paragraph`() {
    doTest(
      "vip",
      """
        |Fi${c}rst paragraph line one.
        |First paragraph line two.
        |
        |Second paragraph.
      """.trimMargin(),
      """
        |${s}First paragraph line one.
        |${c}First paragraph line two.
        |${se}
        |Second paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test inner paragraph on last paragraph`() {
    doTest(
      "vip",
      """
        |First paragraph.
        |
        |Second para${c}graph.
      """.trimMargin(),
      """
        |First paragraph.
        |
        |${s}${c}Second paragraph.${se}
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  // ============== Outer Paragraph (ap) ==============

  @Test
  fun `test outer paragraph from middle of paragraph`() {
    doTest(
      "vap",
      """
        |First paragraph.
        |
        |Second para${c}graph line one.
        |Second paragraph line two.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |
        |${s}Second paragraph line one.
        |Second paragraph line two.
        |${c}
        |${se}Third paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test outer paragraph with backwards selection`() {
    // Cursor moves up to empty line, ap selects from there
    doTest(
      listOf("v", "k", "ap"),
      """
        |First paragraph.
        |
        |Second para${c}graph line one.
        |Second paragraph line two.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |${s}
        |Second paragraph line one.
        |${c}Second paragraph line two.
        |${se}
        |Third paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test outer paragraph on first paragraph`() {
    doTest(
      "vap",
      """
        |Fi${c}rst paragraph.
        |
        |Second paragraph.
      """.trimMargin(),
      """
        |${s}First paragraph.
        |${c}
        |${se}Second paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test outer paragraph on last paragraph`() {
    doTest(
      "vap",
      """
        |First paragraph.
        |
        |Second para${c}graph.
      """.trimMargin(),
      """
        |First paragraph.
        |${s}
        |${c}Second paragraph.${se}
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  // ============== Repeated Paragraph Text Object ==============

  @Test
  fun `test repeated inner paragraph expands selection`() {
    // Current IdeaVim behavior: repeated ip just reselects current paragraph
    doTest(
      listOf("vip", "ip"),
      """
        |First paragraph.
        |
        |Second para${c}graph.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |
        |${s}${c}Second paragraph.
        |${se}
        |Third paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test repeated outer paragraph expands selection`() {
    doTest(
      listOf("vap", "ap"),
      """
        |First paragraph.
        |
        |Second para${c}graph.
        |
        |Third paragraph.
        |
        |Fourth paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |
        |Second paragraph.
        |${s}
        |${c}Third paragraph.
        |${se}
        |Fourth paragraph.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  // ============== Delete with Paragraph Text Object ==============

  @Test
  fun `test delete inner paragraph`() {
    doTest(
      "dip",
      """
        |First paragraph.
        |
        |Second para${c}graph.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |
        |${c}
        |Third paragraph.
      """.trimMargin(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test delete outer paragraph`() {
    doTest(
      "dap",
      """
        |First paragraph.
        |
        |Second para${c}graph.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |
        |${c}Third paragraph.
      """.trimMargin(),
      Mode.NORMAL(),
    )
  }

  // ============== Change with Paragraph Text Object ==============

  @Test
  fun `test change inner paragraph`() {
    doTest(
      "cip",
      """
        |First paragraph.
        |
        |Second para${c}graph.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |
        |${c}
        |
        |Third paragraph.
      """.trimMargin(),
      Mode.INSERT,
    )
  }

  // ============== Yank with Paragraph Text Object ==============

  @Test
  fun `test yank inner paragraph`() {
    doTest(
      "yip",
      """
        |First paragraph.
        |
        |Second para${c}graph.
        |
        |Third paragraph.
      """.trimMargin(),
      """
        |First paragraph.
        |
        |${c}Second paragraph.
        |
        |Third paragraph.
      """.trimMargin(),
      Mode.NORMAL(),
    )
  }
}

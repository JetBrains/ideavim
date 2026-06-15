/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.targets

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Port of targets.vim `s:testGrow` (test9): repeatedly issuing a text object in visual mode grows
 * the selection, counts apply on top of an existing selection, and re-issuing the inner object does
 * not grow. Fixtures/expectations from `test/test9.in` / `test/test9.ok`.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsGrowTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // From `x`, select an argument, then grow it to the enclosing argument.
  @Test
  fun `an argument grows to the enclosing argument`() {
    doTest(
      "vaaaa",
      "a ( b, c ( ${c}x , d ), e )",
      "a ( b$s, c ( x , d )$se, e )",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // From `x`, select the inner argument, then `aa` — should not grow past the current argument.
  @Test
  fun `inside then an argument does not grow extra`() {
    doTest(
      "viaaa",
      "a ( b, c ( ${c}x , d ), e )",
      "a ( b, c ( ${s}x , ${se}d ), e )",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // From `x`, select inner argument, then a second outer argument with a count.
  @Test
  fun `inside then count-2 an argument grows to the outer list`() {
    doTest(
      "via2aa",
      "a ( b, c ( d ( ${c}x , e ) , f ), g )",
      "a ( b, c ( ${s}d ( x , e ) , ${se}f ), g )",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // Repeating `ib` grows through nested blocks.
  @Test
  fun `inside any-block grows through nesting`() {
    doTest(
      "vibib",
      "( ( ( ${c}x ) ) )",
      "( ($s ( x ) $se) )",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}

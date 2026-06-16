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
 * Port of targets.vim `s:testVisual` (test4): re-triggering a text object in visual mode grows the
 * selection outward, and targets' `I`/`A` modifiers cooperate with linewise (`V`) selections while
 * staying out of the way of blockwise (`<C-V>`) block-insert. Fixtures/expectations from
 * `test/test4.in` / `test/test4.ok`.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsVisualTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // `vi(` then `i(` again grows from the inner pair's interior to the outer pair's interior.
  @Test
  fun `repeating inside pair grows selection`() {
    doTest(
      "vi(i(",
      "a ( b ( ${c}x ) c ) d",
      "a ($s b ( x ) c $se) d",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // `va(` then `a(` grows to the whole outer pair.
  @Test
  fun `repeating a pair grows to outer pair`() {
    doTest(
      "va(a(",
      "a ( b ( ${c}x ) c ) d",
      "a $s( b ( x ) c )$se d",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // Same growing behaviour for tags.
  @Test
  fun `repeating inside tag grows selection`() {
    doTest(
      "vitit",
      "a <a> b <b> ${c}x </b> c </a> d",
      "a <a>$s b <b> x </b> c $se</a> d",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `repeating a tag grows to outer tag`() {
    doTest(
      "vatat",
      "a <a> b <b> ${c}x </b> c </a> d",
      "a $s<a> b <b> x </b> c </a>$se d",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

}

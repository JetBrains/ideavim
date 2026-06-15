/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.targets

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Port of targets.vim "Multi Text Objects": the `b` (any block) and `q` (any quote) triggers.
 *
 * `dib` operates on the nearest of `() [] {}`; a count jumps outward to the enclosing block of any
 * kind. `diq` operates on the nearest of `' " ` ``.
 *
 * See the README "Any Block" and "Any Quote" sections.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsAnyBlockQuoteTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  @Test
  fun `any block inside parentheses`() {
    doTest("dib", "{ ( a${c}b ) }", "{ (${c}) }", Mode.NORMAL())
  }

  @Test
  fun `any block inside curly braces`() {
    doTest("dib", "( { a${c}b } )", "( {${c}} )", Mode.NORMAL())
  }

  @Test
  fun `any block inside square brackets`() {
    doTest("dib", "( [ a${c}b ] )", "( [${c}] )", Mode.NORMAL())
  }

  // From within `()` nested inside `{}`, a count of 2 selects the enclosing `{}` block.
  @Test
  fun `any block with count jumps to enclosing block of any kind`() {
    doTest("d2ib", "{ ( a${c}b ) }", "{${c}}", Mode.NORMAL())
  }

  @Test
  fun `any quote inside double quotes`() {
    doTest("diq", "'outer' \"a${c}b\"", "'outer' \"$c\"", Mode.NORMAL())
  }

  @Test
  fun `any quote inside single quotes`() {
    doTest("diq", "\"outer\" 'a${c}b'", "\"outer\" '$c'", Mode.NORMAL())
  }

  @Test
  fun `any quote inside back ticks`() {
    doTest("diq", "'outer' `a${c}b`", "'outer' `$c`", Mode.NORMAL())
  }
}

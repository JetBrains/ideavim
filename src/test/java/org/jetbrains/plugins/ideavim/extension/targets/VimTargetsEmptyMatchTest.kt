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
 * Port of targets.vim `s:testEmpty` (test6): a zero-width match (e.g. an empty pair of quotes)
 * cannot be visually selected, but for `c`/`d` the cursor is placed between the delimiters so the
 * user can start typing. Fixtures/expectations from `test/test6.in` / `test/test6.ok`.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsEmptyMatchTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  @Test
  fun `change inside empty double quotes`() {
    doTest("ci\"", "a ${c}\"\" b", "a \"$c\" b", Mode.INSERT)
  }

  @Test
  fun `change inside empty parentheses`() {
    doTest("ci(", "a ${c}() b", "a ($c) b", Mode.INSERT)
  }

  @Test
  fun `change inside empty separators`() {
    doTest("ci,", "a ${c},, b", "a ,$c, b", Mode.INSERT)
  }
}

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
 * Port of targets.vim `s:testReselect` (test8): when a text object fails in visual mode, the
 * previous selection is restored rather than lost. Here `viw` selects a word, the failing `ab`
 * (no surrounding block) leaves the word still selected, so the following `c` changes the word.
 * Fixture/expectation from `test/test8.in` / `test/test8.ok`.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsReselectTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  @Test
  fun `failed text object in visual mode keeps previous selection`() {
    doTest(
      "viwabcfoo",
      "${c}word another",
      "foo$c another",
      Mode.INSERT,
    )
  }
}

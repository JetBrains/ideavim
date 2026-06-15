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
 * Port of targets.vim `s:testMotionForce` (test10): the operator-pending motion-force prefixes
 * `v` / `V` / `<C-V>` change how a (multiline) separator text object is applied — characterwise,
 * forced linewise, or forced blockwise. Fixtures/expectations from `test/test10.in` /
 * `test/test10.ok`.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsMotionForceTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // `di-` deletes the multiline interior between the two `-` separators, joining them.
  @Test
  fun `delete inside separator across lines`() {
    doTest(
      "di-",
      """
        111
        aaa - bbb
        ${c}X1
        ddd - eee
        222
      """.trimIndent(),
      """
        111
        aaa -$c- eee
        222
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // `dvi-` forces characterwise — same effect here.
  @Test
  fun `delete inside separator forced characterwise`() {
    doTest(
      "dvi-",
      """
        111
        aaa - bbb
        ${c}X2
        ddd - eee
        222
      """.trimIndent(),
      """
        111
        aaa -$c- eee
        222
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // `dVi-` forces linewise — removes the whole span of lines.
  @Test
  fun `delete inside separator forced linewise`() {
    doTest(
      "dVi-",
      """
        111
        aaa - bbb
        ${c}X3
        ddd - eee
        222
      """.trimIndent(),
      """
        111
        ${c}222
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // `d<C-V>i-` forces blockwise — removes the ` - ` column rectangle.
  @Test
  fun `delete inside separator forced blockwise`() {
    doTest(
      "d<C-V>i-",
      """
        111
        aaa - bbb
        ${c}X4
        ddd - eee
        222
      """.trimIndent(),
      """
        111
        aaa${c}bbb
        X4
        dddeee
        222
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}

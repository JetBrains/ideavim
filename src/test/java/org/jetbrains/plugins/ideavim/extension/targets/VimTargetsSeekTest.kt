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
 * Port of targets.vim `s:testSeeking` (test3): when the cursor is not inside a target, a plain
 * command (no `n`/`l`) seeks to the nearest target before or after the cursor, including across
 * lines. Fixtures and expected results derived from `test/test3.in` / `test/test3.ok`.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsSeekTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // `cia` seeks forward on the line to the argument in `(x)`.
  @Test
  fun `argument seek forward on line`() {
    doTest("cia", ", ${c}P (x)", ", P ($c)", Mode.INSERT)
  }

  // `cia` seeks backward on the line to the argument in `(x)`.
  @Test
  fun `argument seek backward on line`() {
    doTest("cia", "(x) ${c}Q ,", "($c) Q ,", Mode.INSERT)
  }

  // `ci)` seeks down to a pair on a following line.
  @Test
  fun `pair seek down to next line`() {
    doTest(
      "ci)",
      """
        ${c}A
        a ( b ) c
      """.trimIndent(),
      """
        A
        a ($c) c
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  // `ci)` seeks forward on the current line.
  @Test
  fun `pair seek forward on line`() {
    doTest("ci)", "B ${c}( a ) b", "B ($c) b", Mode.INSERT)
  }

  // `ci)` seeks backward to a pair earlier on the line.
  @Test
  fun `pair seek backward on line`() {
    doTest("ci)", "a ( b ) ${c}C", "a ($c) C", Mode.INSERT)
  }

  // `ci)` selects a multiline interior when the pair spans lines.
  @Test
  fun `pair seek across multiline interior`() {
    doTest(
      "ci)",
      """
        ${c}D ( a
        b ) c
      """.trimIndent(),
      "D ($c) c",
      Mode.INSERT,
    )
  }

  // `ci'` seeks to a quote on the current line.
  @Test
  fun `quote seek on line`() {
    doTest(
      "ci'",
      """
        J
        a 'b' ${c}c
      """.trimIndent(),
      """
        J
        a '$c' c
      """.trimIndent(),
      Mode.INSERT,
    )
  }
}

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
 * Port of targets.vim `s:testMultiline` (test2): text objects work over multiple lines and seek
 * across lines. The original test wraps each operation with mark-jump scaffolding (`''A barN`) to
 * label the golden output; here we assert the core seek/change effect directly. Fixtures and
 * expected text are taken from `test/test2.in` / `test/test2.ok`.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsMultilineTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // `din{` seeks down to the next brace block and deletes its (multiline) interior.
  @Test
  fun `delete inside next brace block across lines`() {
    doTest(
      "din{",
      """
        // ${c}comment
        function f() {
                return 4;
            }
      """.trimIndent(),
      """
        // comment
        function f() {
            ${c}}
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // `cin{` seeks across lines to a single-line block and changes its interior.
  @Test
  fun `change inside next brace block on a later line`() {
    doTest(
      "cin{",
      """
        // ${c}comment
        foo { bar }
      """.trimIndent(),
      """
        // comment
        foo {$c}
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  // `cin;` seeks down from one line to the next `;` separator pair on a later line.
  @Test
  fun `change inside next semicolon separator across lines`() {
    doTest(
      "cin;",
      """
        // ${c}comment
        a ; b ; c
      """.trimIndent(),
      """
        // comment
        a ;$c; c
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  // `cin\`` seeks down to a multiline template string and changes its interior.
  @Test
  fun `change inside next back tick across lines`() {
    doTest(
      "cin`",
      """
        // ${c}comment
        string x = `line 1
        line 2
        line 3`;
      """.trimIndent(),
      """
        // comment
        string x = `$c`;
      """.trimIndent(),
      Mode.INSERT,
    )
  }
}

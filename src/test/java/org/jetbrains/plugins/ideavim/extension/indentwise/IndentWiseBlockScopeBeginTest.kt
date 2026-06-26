/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.indentwise

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class IndentWiseBlockScopeBeginTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("vim-indentwise")
  }

  @Test
  fun `test block scope begin moves to top of block`() {
    val before = """
      |def foo():
      |    if cond:
      |        a()
      |        ${c}b()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        ${c}a()
      |        b()
      |    return
    """.trimMargin()
    doTest("[%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope begin skips blank lines`() {
    val before = """
      |def foo():
      |    if cond:
      |        a()
      |
      |        ${c}b()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        ${c}a()
      |
      |        b()
      |    return
    """.trimMargin()
    doTest("[%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope begin skips whitespace-only lines`() {
    val before = "    top()\n        a()\n    \n        ${c}b()"
    val after = "    top()\n        ${c}a()\n    \n        b()"
    doTest("[%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope begin does nothing when already at top of block`() {
    val before = """
      |def foo():
      |    if cond:
      |        ${c}a()
      |        b()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        ${c}a()
      |        b()
      |    return
    """.trimMargin()
    doTest("[%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope begin with count moves to outer scope`() {
    val before = """
      |def foo():
      |    if a:
      |        x()
      |        ${c}y()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    ${c}if a:
      |        x()
      |        y()
      |    return
    """.trimMargin()
    doTest("2[%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope begin falls back to first line when no lesser indent above`() {
    val before = """
      |        x()
      |        ${c}y()
    """.trimMargin()
    val after = """
      |        ${c}x()
      |        y()
    """.trimMargin()
    doTest("[%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test operator-pending delete is linewise and includes the whole block`() {
    val before = """
      |def foo():
      |    if cond:
      |        a()
      |        ${c}b()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |    ${c}return
    """.trimMargin()
    doTest("d[%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test visual mode extends charwise selection to top of block`() {
    val before = """
      |def foo():
      |    if cond:
      |        a()
      |        ${c}b()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        ${s}${c}a()
      |        b${se}()
      |    return
    """.trimMargin()
    doTest("v[%", before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}

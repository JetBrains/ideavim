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

class IndentWiseBlockScopeEndTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("indentwise")
  }

  @Test
  fun `test block scope end moves to bottom of block`() {
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
      |        a()
      |        ${c}b()
      |    return
    """.trimMargin()
    doTest("]%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope end skips blank lines`() {
    val before = """
      |def foo():
      |    if cond:
      |        ${c}a()
      |
      |        b()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        a()
      |
      |        ${c}b()
      |    return
    """.trimMargin()
    doTest("]%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope end skips whitespace-only lines`() {
    val before = "        ${c}a()\n    \n        b()\n    bottom()"
    val after = "        a()\n    \n        ${c}b()\n    bottom()"
    doTest("]%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope end does nothing when already at bottom of block`() {
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
      |        a()
      |        ${c}b()
      |    return
    """.trimMargin()
    doTest("]%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope end with count moves to outer scope`() {
    val before = """
      |def foo():
      |    if a:
      |        ${c}x()
      |        y()
      |    z()
      |def bar():
    """.trimMargin()
    val after = """
      |def foo():
      |    if a:
      |        x()
      |        y()
      |    ${c}z()
      |def bar():
    """.trimMargin()
    doTest("2]%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test block scope end falls back to last line when no lesser indent below`() {
    val before = """
      |        ${c}x()
      |        y()
    """.trimMargin()
    val after = """
      |        x()
      |        ${c}y()
    """.trimMargin()
    doTest("]%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test operator-pending delete is linewise and includes the whole block`() {
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
      |    ${c}return
    """.trimMargin()
    doTest("d]%", before, after, Mode.NORMAL())
  }

  @Test
  fun `test visual mode extends charwise selection to bottom of block`() {
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
      |        ${s}a()
      |        ${c}b${se}()
      |    return
    """.trimMargin()
    doTest("v]%", before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}

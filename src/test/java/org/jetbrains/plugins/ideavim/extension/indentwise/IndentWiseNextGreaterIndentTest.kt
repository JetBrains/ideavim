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

class IndentWiseNextGreaterIndentTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("vim-indentwise")
  }

  @Test
  fun `test next greater indent`() {
    val before = """
      |def foo():
      |    ${c}if cond:
      |        bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |        ${c}bar()
    """.trimMargin()
    doTest("]+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next greater indent skips blank lines`() {
    val before = """
      |def foo():
      |    ${c}if cond:
      |
      |        bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    if cond:
      |
      |        ${c}bar()
    """.trimMargin()
    doTest("]+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next greater indent skips lines of same or lesser indent`() {
    val before = """
      |    ${c}d()
      |    c()
      |b()
      |        a()
    """.trimMargin()
    val after = """
      |    d()
      |    c()
      |b()
      |        ${c}a()
    """.trimMargin()
    doTest("]+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next greater indent does nothing at last line`() {
    val before = """
      |        bar()
      |    ${c}foo()
    """.trimMargin()
    val after = """
      |        bar()
      |    ${c}foo()
    """.trimMargin()
    doTest("]+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next greater indent with count`() {
    val before = """
      |    ${c}shallow()
      |        mid()
      |            deep()
    """.trimMargin()
    val after = """
      |    shallow()
      |        mid()
      |            ${c}deep()
    """.trimMargin()
    doTest("2]+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next greater indent does nothing when only lesser indent below`() {
    val before = """
      |        ${c}y()
      |    x()
    """.trimMargin()
    val after = """
      |        ${c}y()
      |    x()
    """.trimMargin()
    doTest("]+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next greater indent skips whitespace-only lines`() {
    val before = "    ${c}baz()\n    \n        bar()"
    val after = "    baz()\n    \n        ${c}bar()"
    doTest("]+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test operator-pending delete is linewise and excludes the greater-indent line`() {
    val before = """
      |def foo():
      |    ${c}b()
      |    a()
      |        bar()
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |        ${c}bar()
      |    return
    """.trimMargin()
    doTest("d]+", before, after, Mode.NORMAL())
  }

  @Test
  fun `test visual mode extends charwise selection to greater-indent line`() {
    val before = """
      |def foo():
      |    ${c}if cond:
      |        bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    ${s}if cond:
      |        ${c}b${se}ar()
    """.trimMargin()
    doTest("v]+", before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}

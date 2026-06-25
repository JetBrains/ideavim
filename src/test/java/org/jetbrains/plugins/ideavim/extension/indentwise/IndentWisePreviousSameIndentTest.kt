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

class IndentWisePreviousSameIndentTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("vim-indentwise")
  }

  @Test
  fun `test previous same indent`() {
    val before = """
      |def foo():
      |    a = 1
      |    ${c}b = 2
    """.trimMargin()
    val after = """
      |def foo():
      |    ${c}a = 1
      |    b = 2
    """.trimMargin()
    doTest("[=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous same indent skips blank lines`() {
    val before = """
      |def foo():
      |    a = 1
      |
      |    ${c}b = 2
    """.trimMargin()
    val after = """
      |def foo():
      |    ${c}a = 1
      |
      |    b = 2
    """.trimMargin()
    doTest("[=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous same indent skips lines of different indent`() {
    val before = """
      |    a = 1
      |        deep()
      |shallow()
      |    ${c}b = 2
    """.trimMargin()
    val after = """
      |    ${c}a = 1
      |        deep()
      |shallow()
      |    b = 2
    """.trimMargin()
    doTest("[=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous same indent does nothing at first line`() {
    val before = """
      |    ${c}foo()
      |        bar()
    """.trimMargin()
    val after = """
      |    ${c}foo()
      |        bar()
    """.trimMargin()
    doTest("[=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous same indent with count`() {
    val before = """
      |    a = 1
      |    b = 2
      |    c = 3
      |    ${c}d = 4
    """.trimMargin()
    val after = """
      |    a = 1
      |    ${c}b = 2
      |    c = 3
      |    d = 4
    """.trimMargin()
    doTest("2[=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous same indent does nothing when no line of same indent above`() {
    val before = """
      |        x()
      |    ${c}y()
    """.trimMargin()
    val after = """
      |        x()
      |    ${c}y()
    """.trimMargin()
    doTest("[=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous same indent skips whitespace-only lines`() {
    val before = "    a = 1\n    \n    ${c}b = 2"
    val after = "    ${c}a = 1\n    \n    b = 2"
    doTest("[=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test operator-pending delete is linewise and excludes the same-indent line`() {
    val before = """
      |def foo():
      |    a = 1
      |        deep1()
      |        deep2()
      |    ${c}b = 2
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    a = 1
      |    ${c}return
    """.trimMargin()
    doTest("d[=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test visual mode extends charwise selection to same-indent line`() {
    val before = """
      |def foo():
      |    a = 1
      |    ${c}b = 2
    """.trimMargin()
    val after = """
      |def foo():
      |    ${s}${c}a = 1
      |    b${se} = 2
    """.trimMargin()
    doTest("v[=", before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}

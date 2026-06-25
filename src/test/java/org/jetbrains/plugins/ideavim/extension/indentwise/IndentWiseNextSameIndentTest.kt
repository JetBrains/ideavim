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

class IndentWiseNextSameIndentTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("vim-indentwise")
  }

  @Test
  fun `test next same indent`() {
    val before = """
      |def foo():
      |    ${c}a = 1
      |    b = 2
    """.trimMargin()
    val after = """
      |def foo():
      |    a = 1
      |    ${c}b = 2
    """.trimMargin()
    doTest("]=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next same indent skips blank lines`() {
    val before = """
      |def foo():
      |    ${c}a = 1
      |
      |    b = 2
    """.trimMargin()
    val after = """
      |def foo():
      |    a = 1
      |
      |    ${c}b = 2
    """.trimMargin()
    doTest("]=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next same indent skips lines of different indent`() {
    val before = """
      |    ${c}a = 1
      |        deep()
      |shallow()
      |    b = 2
    """.trimMargin()
    val after = """
      |    a = 1
      |        deep()
      |shallow()
      |    ${c}b = 2
    """.trimMargin()
    doTest("]=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next same indent does nothing at last line`() {
    val before = """
      |        bar()
      |    ${c}foo()
    """.trimMargin()
    val after = """
      |        bar()
      |    ${c}foo()
    """.trimMargin()
    doTest("]=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next same indent with count`() {
    val before = """
      |    ${c}a = 1
      |    b = 2
      |    c = 3
      |    d = 4
    """.trimMargin()
    val after = """
      |    a = 1
      |    b = 2
      |    ${c}c = 3
      |    d = 4
    """.trimMargin()
    doTest("2]=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next same indent does nothing when no line of same indent below`() {
    val before = """
      |    ${c}y()
      |        x()
    """.trimMargin()
    val after = """
      |    ${c}y()
      |        x()
    """.trimMargin()
    doTest("]=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test next same indent skips whitespace-only lines`() {
    val before = "    ${c}a = 1\n    \n    b = 2"
    val after = "    a = 1\n    \n    ${c}b = 2"
    doTest("]=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test operator-pending delete is linewise and excludes the same-indent line`() {
    val before = """
      |def foo():
      |    ${c}a = 1
      |        deep1()
      |        deep2()
      |    b = 2
      |    return
    """.trimMargin()
    val after = """
      |def foo():
      |    ${c}b = 2
      |    return
    """.trimMargin()
    doTest("d]=", before, after, Mode.NORMAL())
  }

  @Test
  fun `test visual mode extends charwise selection to same-indent line`() {
    val before = """
      |def foo():
      |    ${c}a = 1
      |    b = 2
    """.trimMargin()
    val after = """
      |def foo():
      |    ${s}a = 1
      |    ${c}b${se} = 2
    """.trimMargin()
    doTest("v]=", before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}

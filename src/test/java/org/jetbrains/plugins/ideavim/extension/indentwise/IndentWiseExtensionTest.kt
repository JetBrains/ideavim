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

class IndentWiseExtensionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("vim-indentwise")
  }

  @Test
  fun `test previous lesser indent`() {
    val before = """
      |def foo():
      |    if cond:
      |        ${c}bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    ${c}if cond:
      |        bar()
    """.trimMargin()
    doTest("[-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous lesser indent skips blank lines`() {
    val before = """
      |def foo():
      |    if cond:
      |
      |        ${c}bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    ${c}if cond:
      |
      |        bar()
    """.trimMargin()
    doTest("[-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous lesser indent skips lines of same or greater indent`() {
    val before = """
      |def foo():
      |    a = 1
      |    if cond:
      |        baz()
      |        ${c}bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    a = 1
      |    ${c}if cond:
      |        baz()
      |        bar()
    """.trimMargin()
    doTest("[-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous lesser indent does nothing at top level`() {
    val before = """
      |def ${c}foo():
      |    bar()
    """.trimMargin()
    val after = """
      |def ${c}foo():
      |    bar()
    """.trimMargin()
    doTest("[-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous lesser indent with count`() {
    val before = """
      |def foo():
      |    if a:
      |        if b:
      |            ${c}bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    ${c}if a:
      |        if b:
      |            bar()
    """.trimMargin()
    doTest("2[-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous lesser indent does nothing when only greater indent above`() {
    val before = """
      |        x()
      |    ${c}y()
    """.trimMargin()
    val after = """
      |        x()
      |    ${c}y()
    """.trimMargin()
    doTest("[-", before, after, Mode.NORMAL())
  }

  @Test
  fun `test previous lesser indent skips whitespace-only lines`() {
    val before = "def foo():\n    if cond:\n    \n        ${c}bar()"
    val after = "def foo():\n    ${c}if cond:\n    \n        bar()"
    doTest("[-", before, after, Mode.NORMAL())
  }

  // Original vim-indentwise maps `[-` in operator-pending mode with a leading `V`, so the
  // operation is LINEWISE. The exclusive flag pulls the target back one line, so the
  // lesser-indent line itself is NOT included — only the block from the cursor up to it.
  @Test
  fun `test operator-pending delete is linewise and excludes the lesser-indent line`() {
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
    doTest("d[-", before, after, Mode.NORMAL())
  }

  // Original maps `[-` in visual mode via vnoremap (charwise): it moves the cursor to the
  // first non-blank of the lesser-indent line and extends the existing selection.
  @Test
  fun `test visual mode extends charwise selection to lesser-indent line`() {
    val before = """
      |def foo():
      |    if cond:
      |        ${c}bar()
    """.trimMargin()
    val after = """
      |def foo():
      |    ${s}${c}if cond:
      |        b${se}ar()
    """.trimMargin()
    doTest("v[-", before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}

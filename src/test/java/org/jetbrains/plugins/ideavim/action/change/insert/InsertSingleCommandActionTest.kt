/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertSingleCommandActionTest : SingleCommandActionTest() {
  override val command: String = "i"
  override val mode: Mode = Mode.INSERT

  @Test
  fun `test ctrl-o db at end of line`() {
    doTest(
      listOf(command, "<C-O>", "db"),
      "first line${c}\nsecond line",
      "first ${c}e\nsecond line",
      mode,
    )
  }

  @Test
  fun `test ctrl-o x at end of line`() {
    doTest(
      listOf(command, "<C-O>", "x"),
      "first line${c}\nsecond line",
      "first lin${c}\nsecond line",
      mode,
    )
  }

  @Test
  fun `test ctrl-o D at end of line`() {
    doTest(
      listOf(command, "<C-O>", "D"),
      "first line${c}\nsecond line",
      "first lin${c}\nsecond line",
      mode,
    )
  }

  @Test
  fun `test ctrl-o db not at end of line`() {
    doTest(
      listOf(command, "<C-O>", "db"),
      "first li${c}ne\nsecond line",
      "first ${c}ne\nsecond line",
      mode,
    )
  }

  @Test
  fun `test ctrl-o x not at end of line`() {
    doTest(
      listOf(command, "<C-O>", "x"),
      "first li${c}ne\nsecond line",
      "first l${c}ne\nsecond line",
      mode,
    )
  }
}

class ReplaceSingleCommandActionTest : SingleCommandActionTest() {
  override val command: String = "R"
  override val mode: Mode = Mode.REPLACE

  @Test
  fun `test ctrl-o db at end of line`() {
    doTest(
      listOf(command, "<C-O>", "db"),
      "first line${c}\nsecond line",
      "first ${c}e\nsecond line",
      mode,
    )
  }

  @Test
  fun `test ctrl-o x at end of line`() {
    doTest(
      listOf(command, "<C-O>", "x"),
      "first line${c}\nsecond line",
      "first lin${c}\nsecond line",
      mode,
    )
  }

  @Test
  fun `test ctrl-o x not at end of line`() {
    doTest(
      listOf(command, "<C-O>", "x"),
      "first li${c}ne\nsecond line",
      "first l${c}ne\nsecond line",
      mode,
    )
  }
}

abstract class SingleCommandActionTest : VimTestCase() {

  abstract val command: String
  abstract val mode: Mode

  @Test
  fun `one operation`() {
    doTest(
      listOf(command, "<C-O>", "l"),
      "I found ${c}it in a legendary land",
      "I found i${c}t in a legendary land",
      mode,
    )
  }

  @Test
  fun `test enter visual`() {
    doTest(
      listOf(command, "<C-O>", "vlll", "<Esc>"),
      "I found ${c}it in a legendary land",
      "I found it ${c}in a legendary land",
      mode,
    )
  }

}

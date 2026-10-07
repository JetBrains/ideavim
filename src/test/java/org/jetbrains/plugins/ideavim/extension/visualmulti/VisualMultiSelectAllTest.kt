/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.junit.jupiter.api.Test

/**
 * Select All and Visual All (`\\A`)
 */
class VisualMultiSelectAllTest : VisualMultiTestCase() {

  @Test
  fun `test leader A selects all whole word occurrences`() {
    val before = """Int
      |Integer
      |I${c}nt
      |Integer
      |Int
    """.trimMargin()
    configureByText(before)

    typeText("<Bslash><Bslash>A")

    val after = """${s}Int$se
      |Integer
      |${s}Int$se
      |Integer
      |${s}Int$se
    """.trimMargin()
    assertState(after)
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test leader A in visual mode selects all occurrences of selection`() {
    val before = """Int
      |Integer
      |${c}Int
      |Integer
      |Int
    """.trimMargin()
    configureByText(before)

    typeText("vll" + "<Bslash><Bslash>A")

    val after = """${s}Int$se
      |${s}Int${se}eger
      |${s}Int$se
      |${s}Int${se}eger
      |${s}Int$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test default leader is two backslashes`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)

    typeText("<Bslash>A")

    // A single backslash is not the leader, so `A` is the normal append command
    assertState(
      """qwe
      |asd
      |qwe$c
      """.trimMargin(),
    )
    assertMode(Mode.INSERT)
  }
}

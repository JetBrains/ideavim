/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.api.Test

/**
 * `g:VM_leader`
 */
class VisualMultiLeaderTest : VisualMultiTestCase() {

  @Test
  fun `test VM_leader changes leader of Select All`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = ','")

    typeText(",A")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_leader changes leader of Visual All`() {
    val before = """Int
      |Integer
      |${c}Int
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = ','")

    typeText("vll" + ",A")

    val after = """${s}Int$se
      |${s}Int${se}eger
      |${s}Int$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_leader removes mapping with default leader`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = ','")

    typeText("<Bslash><Bslash>A")

    // Without the mapping, `A` is the normal append command
    assertState(
      """qwe
      |asd
      |qwe$c
      """.trimMargin(),
    )
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test VM_leader set to single backslash`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = '\\'")

    typeText("<Bslash>A")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_leader with key notation`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = '<Space>'")

    typeText("<Space>A")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_leader does not change ctrl-n mapping`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = ','")

    typeText("<C-n><C-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_leader is not used when default mappings disabled`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = ','", "let g:VM_default_mappings = 0")

    typeText(",A")

    // `,` repeats the last f/t search (there is none), and `A` is the normal append command
    assertState(
      """qwe
      |asd
      |qwe$c
      """.trimMargin(),
    )
    assertMode(Mode.INSERT)
  }
}

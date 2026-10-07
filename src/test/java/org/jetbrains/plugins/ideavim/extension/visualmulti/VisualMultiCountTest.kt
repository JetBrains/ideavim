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
 * `[count]` for Find Under
 */
class VisualMultiCountTest : VisualMultiTestCase() {

  @Test
  fun `test count for ctrl-n selects that many occurrences`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("3<C-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test count for ctrl-n uses whole word occurrences`() {
    val before = """I${c}nt
      |Integer
      |Int
      |Integer
      |Int
    """.trimMargin()
    configureByText(before)

    typeText("3<C-n>")

    val after = """${s}Int$se
      |Integer
      |${s}Int$se
      |Integer
      |${s}Int$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test count for ctrl-n larger than number of occurrences`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("5<C-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test count for ctrl-n adds that many occurrences to existing selections`() {
    // Inside a vim-visual-multi session, <C-n> on a region is "find next", and it accepts a count
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("<C-n>" + "2<C-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test count for ctrl-n of one is same as no count`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("1<C-n>")

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test count for Find Under mapped with VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = '<C-d>'")

    typeText("3<C-d>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test count for Find Under plug mapping`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("2<Plug>(VM-Find-Under)")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }
}

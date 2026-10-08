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
 * Find Under and Find Subword Under (`<C-n>`)
 */
class VisualMultiFindUnderTest : VisualMultiTestCase() {

  @Test
  fun `test ctrl-n selects word under caret`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("<C-n>")

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test ctrl-n adds next whole word occurrence`() {
    val before = """I${c}nt
      |Integer
      |Int
      |Integer
      |Int
    """.trimMargin()
    configureByText(before)

    typeText("<C-n><C-n>")

    val after = """${s}Int$se
      |Integer
      |${s}Int$se
      |Integer
      |Int
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test ctrl-n adds all whole word occurrences`() {
    val before = """I${c}nt
      |Integer
      |Int
      |Integer
      |Int
    """.trimMargin()
    configureByText(before)

    typeText("<C-n><C-n><C-n>")

    val after = """${s}Int$se
      |Integer
      |${s}Int$se
      |Integer
      |${s}Int$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test ctrl-n in visual mode finds subword occurrences`() {
    val before = """${c}qwe
      |asdqweasd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("vll" + "<C-n><C-n>")

    val after = """${s}qwe$se
      |asd${s}qwe${se}asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test first ctrl-n in visual mode only makes the selection a region`() {
    val before = """${c}qwe
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("vll" + "<C-n>")

    val after = """${s}qwe$se
      |qwe
    """.trimMargin()
    assertState(after)
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test ctrl-n on multiline selection finds multiline occurrences`() {
    // vim-multiple-cursors would put a cursor on each line instead
    val before = """${c}ab
      |cd
      |ab
      |cd
    """.trimMargin()
    configureByText(before)

    typeText("vj" + "<C-n><C-n>")

    val after = """${s}ab
      |c${se}d
      |${s}ab
      |c${se}d
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test ctrl-n does not add caret when there are no more matches`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("<C-n>".repeat(4))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test change all occurrences found with ctrl-n`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText("<C-n><C-n><C-n>" + "cfoo<Esc>")

    val after = """fo${c}o
      |asd
      |fo${c}o
      |asd
      |fo${c}o
    """.trimMargin()
    assertState(after)
    assertMode(Mode.NORMAL())
  }
}

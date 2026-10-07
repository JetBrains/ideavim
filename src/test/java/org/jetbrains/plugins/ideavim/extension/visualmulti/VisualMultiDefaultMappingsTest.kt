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
 * The default mappings, `g:VM_default_mappings` and enabling the extension
 */
class VisualMultiDefaultMappingsTest : VisualMultiTestCase() {

  @Test
  fun `test vim-multiple-cursors mappings are not created`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)

    typeText("<A-n>")

    assertState(before)
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test VM_default_mappings zero keeps ctrl-n mapping`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_default_mappings = 0")

    typeText("<C-n><C-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_default_mappings zero removes leader A mapping`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_default_mappings = 0")

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
  fun `test plug mappings still available when default mappings disabled`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_default_mappings = 0")

    typeText("<Plug>(VM-Select-All)")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test user can map Find Under to another key`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("nmap <C-d> <Plug>(VM-Find-Under)")

    typeText("<C-d>")

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test default ctrl-n mapping is not created when Find Under is mapped by user`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("nmap <C-d> <Plug>(VM-Find-Under)")

    typeText("<C-n>")

    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test enabled with Plug command`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("set novisual-multi")
    executeVimscript("Plug 'mg979/vim-visual-multi'")

    typeText("<C-n><C-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }
}

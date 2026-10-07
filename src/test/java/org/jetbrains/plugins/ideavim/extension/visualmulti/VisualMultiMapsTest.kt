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
 * `g:VM_maps` for the permanent mappings
 */
class VisualMultiMapsTest : VisualMultiTestCase() {

  @Test
  fun `test VM_maps changes Find Under key`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = '<C-d>'")

    typeText("<C-d>")

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps removes default Find Under key`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = '<C-d>'")

    typeText("<C-n>")

    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test VM_maps with Sublime Text like mappings`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter(
      "let g:VM_maps = {}",
      "let g:VM_maps['Find Under'] = '<C-d>'",
      "let g:VM_maps['Find Subword Under'] = '<C-d>'",
    )

    typeText("<C-d><C-d><C-d>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps defined with dictionary literal`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {'Find Under': '<C-d>', 'Find Subword Under': '<C-d>'}")

    typeText("<C-d><C-d>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps changes Select All and Visual All keys`() {
    val before = """Int
      |Integer
      |I${c}nt
    """.trimMargin()
    configureByText(before)
    reinitAfter(
      "let g:VM_maps = {}",
      "let g:VM_maps['Select All'] = '<M-n>'",
      "let g:VM_maps['Visual All'] = '<M-n>'",
    )

    typeText("<M-n>")

    val after = """${s}Int$se
      |Integer
      |${s}Int$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps changes Visual All key`() {
    val before = """Int
      |Integer
      |${c}Int
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Visual All'] = '<M-n>'")

    typeText("vll" + "<M-n>")

    val after = """${s}Int$se
      |${s}Int${se}eger
      |${s}Int$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps removes default Select All key`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Select All'] = '<M-n>'")

    typeText("<Bslash><Bslash>A")

    // Without the mapping, `A` is the normal append command
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test VM_maps empty string disables mapping`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Select All'] = ''")

    typeText("<Bslash><Bslash>A")

    // Without the mapping, `A` is the normal append command
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test VM_maps empty string disables ctrl-n mapping`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = ''")

    typeText("<C-n>")

    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test plug mapping still available when VM_maps disables mapping`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Select All'] = ''")

    typeText("<Plug>(VM-Select-All)")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps key is not prefixed with VM_leader`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = ','", "let g:VM_maps = {}", "let g:VM_maps['Select All'] = 'gA'")

    typeText("gA")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps applies when default mappings disabled`() {
    // vim-visual-multi knows the names of all permanent mappings, so g:VM_maps can still assign keys to them
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter(
      "let g:VM_default_mappings = 0",
      "let g:VM_maps = {}",
      "let g:VM_maps['Select All'] = '<M-n>'",
    )

    typeText("<M-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps ignores unknown mapping names`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['No Such Mapping'] = 'x'")
    assertPluginError(false)

    typeText("<C-n><C-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test VM_maps does not map unknown mapping names`() {
    val before = """q${c}we
      |asd
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['No Such Mapping'] = 'x'")

    typeText("x")

    assertState(
      """q${c}e
      |asd
      """.trimMargin(),
    )
  }
}

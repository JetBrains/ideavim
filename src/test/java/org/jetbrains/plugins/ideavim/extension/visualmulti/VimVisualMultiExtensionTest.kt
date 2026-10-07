/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class VimVisualMultiExtensionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("visual-multi")
  }

  // Find Under / Find Subword Under (<C-n>)

  @Test
  fun `test ctrl-n selects word under caret`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<C-n>"))

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

    typeText(injector.parser.parseKeys("<C-n><C-n>"))

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

    typeText(injector.parser.parseKeys("<C-n><C-n><C-n>"))

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

    typeText(injector.parser.parseKeys("vll" + "<C-n>"))

    val after = """${s}qwe$se
      |asd${s}qwe${se}asd
      |qwe
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

    typeText(injector.parser.parseKeys("<C-n>".repeat(4)))

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

    typeText(injector.parser.parseKeys("<C-n><C-n><C-n>" + "cfoo<Esc>"))

    val after = """fo${c}o
      |asd
      |fo${c}o
      |asd
      |fo${c}o
    """.trimMargin()
    assertState(after)
    assertMode(Mode.NORMAL())
  }

  // Select All / Visual All (\\A)

  @Test
  fun `test leader A selects all whole word occurrences`() {
    val before = """Int
      |Integer
      |I${c}nt
      |Integer
      |Int
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("\\\\A"))

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

    typeText(injector.parser.parseKeys("vll" + "\\\\A"))

    val after = """${s}Int$se
      |${s}Int${se}eger
      |${s}Int$se
      |${s}Int${se}eger
      |${s}Int$se
    """.trimMargin()
    assertState(after)
  }

  // Skip Region / Remove Region. These have no default mappings yet

  @Test
  fun `test skip region plug mapping`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("xmap <C-x> <Plug>(VM-Skip-Region)")

    // Select the first occurrence, skip it to the second, then add the third
    typeText(injector.parser.parseKeys("<C-n>" + "<C-x>" + "<C-n>"))

    val after = """qwe
      |asd
      |${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test remove region plug mapping`() {
    val before = """q${c}we
      |asd
      |qwe
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("xmap <C-p> <Plug>(VM-Remove-Region)")

    // Add three cursors, then remove the last one
    typeText(injector.parser.parseKeys("<C-n>".repeat(3) + "<C-p>"))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test removing the last region exits visual mode`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("xmap <C-p> <Plug>(VM-Remove-Region)")

    typeText(injector.parser.parseKeys("<C-n><C-n>" + "<C-p><C-p>"))

    assertMode(Mode.NORMAL())
  }

  // Mappings

  @Test
  fun `test vim-multiple-cursors mappings are not created`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)

    typeText(injector.parser.parseKeys("<A-n>"))

    assertState(before)
    assertMode(Mode.NORMAL())
  }

  /**
   * Re-runs the extension's [init] after [command], which is what makes `init()` see variables and mappings defined
   * afterward. In the `.ideavimrc` file the extension is initialised after the whole file has been executed.
   */
  private fun reinitAfter(command: String) {
    enterCommand("set novisual-multi")
    enterCommand(command)
    enterCommand("set visual-multi")
  }

  @Test
  fun `test VM_default_mappings zero keeps ctrl-n mapping`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_default_mappings = 0")

    typeText(injector.parser.parseKeys("<C-n><C-n>"))

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

    typeText(injector.parser.parseKeys("\\\\A"))

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

    typeText(injector.parser.parseKeys("<Plug>(VM-Select-All)"))

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

    typeText(injector.parser.parseKeys("<C-d>"))

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

    typeText(injector.parser.parseKeys("<C-n>"))

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

    typeText(injector.parser.parseKeys("<C-n><C-n>"))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }
}

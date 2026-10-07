/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.visualmulti.VimVisualMultiExtension
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.beans.PropertyChangeEvent
import javax.swing.JPanel

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

    typeText(injector.parser.parseKeys("<Bslash><Bslash>A"))

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

    typeText(injector.parser.parseKeys("vll" + "<Bslash><Bslash>A"))

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

    typeText(injector.parser.parseKeys("<Bslash>A"))

    // A single backslash is not the leader, so `A` is the normal append command
    assertState(
      """qwe
      |asd
      |qwe$c
      """.trimMargin(),
    )
    assertMode(Mode.INSERT)
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
   * Re-runs the extension's [init] after [commands], which is what makes `init()` see variables and mappings defined
   * afterward. In the `.ideavimrc` file the extension is initialised after the whole file has been executed.
   */
  private fun reinitAfter(vararg commands: String) {
    enterCommand("set novisual-multi")
    commands.forEach { enterCommand(it) }
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

    typeText(injector.parser.parseKeys("<Bslash><Bslash>A"))

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

  // g:VM_leader

  @Test
  fun `test VM_leader changes leader of Select All`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_leader = ','")

    typeText(injector.parser.parseKeys(",A"))

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

    typeText(injector.parser.parseKeys("vll" + ",A"))

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

    typeText(injector.parser.parseKeys("<Bslash><Bslash>A"))

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

    typeText(injector.parser.parseKeys("<Bslash>A"))

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

    typeText(injector.parser.parseKeys("<Space>A"))

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

    typeText(injector.parser.parseKeys("<C-n><C-n>"))

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

    typeText(injector.parser.parseKeys(",A"))

    // `,` repeats the last f/t search (there is none), and `A` is the normal append command
    assertState(
      """qwe
      |asd
      |qwe$c
      """.trimMargin(),
    )
    assertMode(Mode.INSERT)
  }

  // g:VM_maps (permanent mappings)

  @Test
  fun `test VM_maps changes Find Under key`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = '<C-d>'")

    typeText(injector.parser.parseKeys("<C-d>"))

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

    typeText(injector.parser.parseKeys("<C-n>"))

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

    typeText(injector.parser.parseKeys("<C-d><C-d><C-d>"))

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

    typeText(injector.parser.parseKeys("<C-d><C-d>"))

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

    typeText(injector.parser.parseKeys("<M-n>"))

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

    typeText(injector.parser.parseKeys("vll" + "<M-n>"))

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

    typeText(injector.parser.parseKeys("<Bslash><Bslash>A"))

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

    typeText(injector.parser.parseKeys("<Bslash><Bslash>A"))

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

    typeText(injector.parser.parseKeys("<C-n>"))

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

    typeText(injector.parser.parseKeys("<Plug>(VM-Select-All)"))

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

    typeText(injector.parser.parseKeys("gA"))

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

    typeText(injector.parser.parseKeys("<M-n>"))

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

    typeText(injector.parser.parseKeys("<C-n><C-n>"))

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

    typeText(injector.parser.parseKeys("x"))

    assertState(
      """q${c}e
      |asd
      """.trimMargin(),
    )
  }

  // Reloading g:VM_maps. Changing the variables (e.g. by reloading the ideavimrc) should not require restarting the IDE

  private val extension: VimVisualMultiExtension
    get() = VimExtension.EP_NAME.extensionList.single { it.name == "visual-multi" }.instance as VimVisualMultiExtension

  private fun reloadMappings() {
    ApplicationManager.getApplication().invokeAndWait { extension.reloadMappings() }
  }

  private fun focusOwnerChanged(newFocusOwner: Any) {
    ApplicationManager.getApplication().invokeAndWait {
      extension.focusListener.propertyChange(PropertyChangeEvent(this, "focusOwner", null, newFocusOwner))
    }
  }

  @Test
  fun `test reloading mappings applies changed VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    reloadMappings()
    typeText(injector.parser.parseKeys("<C-d>"))

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test reloading mappings removes key replaced in VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    reloadMappings()
    typeText(injector.parser.parseKeys("<C-n>"))

    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test reloading mappings applies key changed again in VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = '<C-d>'")
    enterCommand("let g:VM_maps['Find Under'] = '<C-e>'")

    reloadMappings()
    typeText(injector.parser.parseKeys("<C-e>"))

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test reloading mappings restores default key removed from VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = '<C-d>'")
    enterCommand("let g:VM_maps = {}")

    reloadMappings()
    typeText(injector.parser.parseKeys("<C-n><C-n>"))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test reloading mappings applies changed VM_default_mappings`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_default_mappings = 0")

    reloadMappings()
    typeText(injector.parser.parseKeys("<Bslash><Bslash>A"))

    // Without the mapping, `A` is the normal append command
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test reloading mappings keeps plug mappings`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Select All'] = ''")

    reloadMappings()
    typeText(injector.parser.parseKeys("<Plug>(VM-Select-All)"))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test reloading mappings does not enable disabled extension`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("set novisual-multi")
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    reloadMappings()
    typeText(injector.parser.parseKeys("<C-d>"))

    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test editor getting focus applies changed VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    focusOwnerChanged(fixture.editor.contentComponent)
    typeText(injector.parser.parseKeys("<C-d>"))

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test other component getting focus does not apply changed VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    focusOwnerChanged(JPanel())
    typeText(injector.parser.parseKeys("<C-n>"))

    // The old mapping is still there
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  // [count]

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

    typeText(injector.parser.parseKeys("3<C-n>"))

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

    typeText(injector.parser.parseKeys("3<C-n>"))

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

    typeText(injector.parser.parseKeys("5<C-n>"))

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

    typeText(injector.parser.parseKeys("<C-n>" + "2<C-n>"))

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

    typeText(injector.parser.parseKeys("1<C-n>"))

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

    typeText(injector.parser.parseKeys("3<C-d>"))

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

    typeText(injector.parser.parseKeys("2<Plug>(VM-Find-Under)"))

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }
}

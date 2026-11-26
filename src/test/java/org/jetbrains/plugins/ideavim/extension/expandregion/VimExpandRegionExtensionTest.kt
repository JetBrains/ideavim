/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.expandregion

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimExpandRegionExtensionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("expand-region")
  }

  @Test
  fun `test expand from normal mode selects word under caret`() {
    configureByText("one tw${c}o three")
    typeText("+")
    assertSelection("two")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test second expand selects inside quotes`() {
    configureByText("""say "one tw${c}o three" now""")
    typeText("++")
    assertSelection("one two three")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test second expand picks the smallest enclosing region`() {
    configureByText("""say "x (one tw${c}o three) y" now""")
    typeText("++")
    assertSelection("one two three")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test third expand moves to the next larger region`() {
    configureByText("""say "x (one tw${c}o three) y" now""")
    typeText("+++")
    assertSelection("x (one two three) y")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test shrink goes back to the previous region`() {
    configureByText("""say "x (one tw${c}o three) y" now""")
    typeText("+++_")
    assertSelection("one two three")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand on the largest region keeps the selection`() {
    configureByText("""say "x (one tw${c}o three) y" now""")
    typeText("+++++")
    assertSelection("""say "x (one two three) y" now""")
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
  }

  @Test
  fun `test repeated shrink walks back through every region`() {
    configureByText("""say "x (one tw${c}o three) y" now""")
    typeText("+++__")
    assertSelection("two")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test shrink on the smallest region leaves visual mode`() {
    configureByText("""say "x (one tw${c}o three) y" now""")
    typeText("+_")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test expand skips a region identical to the current one`() {
    configureByText("""foo("a${c}b", c)""")
    typeText("++")
    assertSelection(""""ab", c""")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand uses WORD between word and quotes`() {
    configureByText("""print(user.name, "hello wor${c}ld")""")
    typeText("++")
    assertSelection("""world")""")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand selects each region from the original caret position`() {
    configureByText("""print(user.name, "hello wor${c}ld")""")
    typeText("+++")
    assertSelection("hello world")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand starts over after the caret moved`() {
    configureByText("""say "one tw${c}o" (three four)""")
    typeText("+<Esc>f(lv+")
    assertSelection("three")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test shrink starts over after the caret moved`() {
    configureByText("""say "one tw${c}o" (three four)""")
    typeText("++<Esc>f(lv_")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test expand skips regions that do not contain the caret`() {
    configureByText("""fo${c}o "bar baz"""")
    typeText("++")
    assertSelection("""foo "bar baz"""")
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
  }

  @Test
  fun `test expand works again after shrinking out of visual mode`() {
    configureByText("""say "one tw${c}o" (three four)""")
    typeText("++<Esc>f(lv_v+")
    assertSelection("three")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand skips a region that ends right before the caret`() {
    configureByText("""say "ab${c}"""")
    typeText("+")
    assertSelection(""""ab"""")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand without any region does nothing`() {
    configureByText("${c}x")
    typeText("+")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test expand walks out through nested parentheses`() {
    configureByText("f(a, g(b, c${c}d))")
    typeText("++++")
    assertSelection("a, g(b, cd)")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand uses text objects provided by other extensions`() {
    enableExtensions("textobj-line")
    configureByText("""  foo "one tw${c}o" bar""")
    typeText("++++")
    assertSelection("""foo "one two" bar""")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand and shrink can be remapped`() {
    configureByText("say \"one tw${c}o three\" now\nnext")
    enterCommand("set noexpand-region")
    enterCommand("nmap K <Plug>(expand_region_expand)")
    enterCommand("xmap K <Plug>(expand_region_expand)")
    enterCommand("xmap J <Plug>(expand_region_shrink)")
    enterCommand("set expand-region")
    typeText("KKKJ")
    assertSelection("one two three")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test plus keeps its default meaning when expand is remapped`() {
    configureByText("say \"one tw${c}o three\" now\nnext")
    enterCommand("set noexpand-region")
    enterCommand("nmap K <Plug>(expand_region_expand)")
    enterCommand("set expand-region")
    typeText("+")
    assertState("say \"one two three\" now\n${c}next")
  }

  @Test
  fun `test text objects can be configured`() {
    configureByText("""say "one tw${c}o three" now""")
    enterCommand("let g:expand_region_text_objects = {'iw': 0, 'ip': 0}")
    typeText("++")
    assertSelection("""say "one two three" now""")
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
  }

  @Test
  fun `test configured text objects can be nested`() {
    configureByText("f(a, g(b, c${c}d))")
    enterCommand("let g:expand_region_text_objects = {'iw': 0, 'ib': 1}")
    typeText("+++")
    assertSelection("a, g(b, cd)")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand ends in select mode when requested`() {
    configureByText("""say "one tw${c}o three" now""")
    enterCommand("let g:expand_region_use_select_mode = 1")
    typeText("+")
    assertSelection("two")
    assertMode(Mode.SELECT(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand and shrink work in select mode`() {
    configureByText("""say "one tw${c}o three" now""")
    enterCommand("let g:expand_region_use_select_mode = 1")
    typeText("+++_")
    assertSelection("one two three")
    assertMode(Mode.SELECT(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test selectmode cmd turns on select mode`() {
    configureByText("""say "one tw${c}o three" now""")
    enterCommand("set selectmode=cmd")
    typeText("++")
    assertSelection("one two three")
    assertMode(Mode.SELECT(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test expand restores the options it changes`() {
    configureByText("""say "one tw${c}o three" now""")
    typeText("++")
    assertTrue(options().wrapscan)
    assertFalse(options().visualbell)
  }

  @Test
  fun `test expand walks through every region of the documented example`() {
    configureByText("""print(user.name, "hello wor${c}ld")""")
    typeText("++++")
    assertSelection("""user.name, "hello world"""")
    typeText("+")
    assertSelection("""print(user.name, "hello world")""")
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
  }
}

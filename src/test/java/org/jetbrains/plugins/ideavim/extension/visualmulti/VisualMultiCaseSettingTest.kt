/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * `g:VM_case_setting`. Empty, the default, follows 'ignorecase' and 'smartcase'. `'smart'`, `'sensitive'` and
 * `'ignore'` override them.
 */
class VisualMultiCaseSettingTest : VisualMultiTestCase() {

  @Test
  fun `test search is case sensitive by default`() {
    val before = "${c}foo Foo foo"
    configureByText(before)

    typeText("<C-n><C-n>")

    assertState("${s}foo$se Foo ${s}foo$se")
  }

  @Test
  fun `test search follows ignorecase by default`() {
    val before = "${c}foo Foo foo"
    configureByText(before)
    enterCommand("set ignorecase")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se ${s}Foo$se foo")
  }

  @Test
  fun `test search follows smartcase by default for lowercase word`() {
    val before = "${c}foo FOO foo"
    configureByText(before)
    enterCommand("set ignorecase smartcase")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se ${s}FOO$se foo")
  }

  @Test
  fun `test search follows smartcase by default for word with uppercase`() {
    val before = "${c}Foo foo Foo"
    configureByText(before)
    enterCommand("set ignorecase smartcase")

    typeText("<C-n><C-n>")

    assertState("${s}Foo$se foo ${s}Foo$se")
  }

  @Test
  fun `test empty VM_case_setting follows ignorecase`() {
    val before = "${c}foo Foo foo"
    configureByText(before)
    enterCommand("set ignorecase")
    enterCommand("let g:VM_case_setting = ''")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se ${s}Foo$se foo")
  }

  @Test
  fun `test VM_case_setting sensitive overrides ignorecase`() {
    val before = "${c}foo Foo foo"
    configureByText(before)
    enterCommand("set ignorecase")
    enterCommand("let g:VM_case_setting = 'sensitive'")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se Foo ${s}foo$se")
  }

  @Test
  fun `test VM_case_setting ignore overrides noignorecase`() {
    val before = "${c}foo Foo foo"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'ignore'")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se ${s}Foo$se foo")
  }

  @Test
  fun `test VM_case_setting ignore overrides smartcase`() {
    val before = "${c}Foo foo Foo"
    configureByText(before)
    enterCommand("set ignorecase smartcase")
    enterCommand("let g:VM_case_setting = 'ignore'")

    typeText("<C-n><C-n>")

    assertState("${s}Foo$se ${s}foo$se Foo")
  }

  @Test
  fun `test VM_case_setting smart ignores case for lowercase word`() {
    val before = "${c}foo FOO foo"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'smart'")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se ${s}FOO$se foo")
  }

  @Test
  fun `test VM_case_setting smart is case sensitive for word with uppercase`() {
    val before = "${c}Foo foo Foo"
    configureByText(before)
    enterCommand("set ignorecase")
    enterCommand("let g:VM_case_setting = 'smart'")

    typeText("<C-n><C-n>")

    assertState("${s}Foo$se foo ${s}Foo$se")
  }

  @Test
  fun `test VM_case_setting value is case insensitive`() {
    // vim-visual-multi compares the value with ==?
    val before = "${c}foo Foo foo"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'IGNORE'")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se ${s}Foo$se foo")
  }

  @Test
  fun `test unknown VM_case_setting follows ignorecase`() {
    val before = "${c}foo Foo foo"
    configureByText(before)
    enterCommand("set ignorecase")
    enterCommand("let g:VM_case_setting = 'unknown'")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se ${s}Foo$se foo")
  }

  @Test
  fun `test VM_case_setting ignore keeps whole word matching`() {
    val before = "${c}foo Foobar FOO"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'ignore'")

    typeText("<C-n><C-n>")

    assertState("${s}foo$se Foobar ${s}FOO$se")
  }

  @Test
  fun `test VM_case_setting ignore adds occurrences with different case`() {
    // The selected texts differ in case, but they are all occurrences of the same pattern
    val before = "${c}foo Foo FOO bar"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'ignore'")

    typeText("<C-n><C-n><C-n>")

    assertState("${s}foo$se ${s}Foo$se ${s}FOO$se bar")
  }

  @Test
  fun `test VM_case_setting smart is decided by the first occurrence`() {
    // The last added occurrence is `FOO`, but the search is still for the lowercase `foo`, which ignores case
    val before = "${c}foo FOO Foo bar"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'smart'")

    typeText("<C-n><C-n><C-n>")

    assertState("${s}foo$se ${s}FOO$se ${s}Foo$se bar")
  }

  @Test
  fun `test VM_case_setting ignore for Select All`() {
    val before = "${c}foo Foo bar FOO"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'ignore'")

    typeText("<Bslash><Bslash>A")

    assertState("${s}foo$se ${s}Foo$se bar ${s}FOO$se")
  }

  @Test
  fun `test VM_case_setting sensitive for Select All`() {
    val before = "${c}foo Foo bar foo"
    configureByText(before)
    enterCommand("set ignorecase")
    enterCommand("let g:VM_case_setting = 'sensitive'")

    typeText("<Bslash><Bslash>A")

    assertState("${s}foo$se Foo bar ${s}foo$se")
  }

  @Test
  fun `test VM_case_setting ignore for Visual All`() {
    val before = "${c}foo xFOOx bar"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'ignore'")

    typeText("vll" + "<Bslash><Bslash>A")

    assertState("${s}foo$se x${s}FOO${se}x bar")
  }

  @Test
  fun `test VM_case_setting ignore for Skip Region`() {
    val before = "${c}foo Foo FOO"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'ignore'")
    enterCommand("xmap <C-x> <Plug>(VM-Skip-Region)")

    // Select the first two occurrences, then skip the second one to the third
    typeText("<C-n><C-n>" + "<C-x>")

    assertState("${s}foo$se Foo ${s}FOO$se")
  }

  @Test
  fun `test VM_case_setting does not change ignorecase option`() {
    // vim-visual-multi changes 'ignorecase' and 'smartcase' for the duration of the session. We apply the setting to
    // the search pattern instead, so the options of the user are never touched
    val before = "${c}foo Foo foo"
    configureByText(before)
    enterCommand("let g:VM_case_setting = 'ignore'")

    typeText("<C-n><C-n>")

    assertFalse(injector.globalOptions().ignorecase)
  }
}

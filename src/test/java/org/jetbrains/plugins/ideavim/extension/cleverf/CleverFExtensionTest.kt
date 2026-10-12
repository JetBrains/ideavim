/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.cleverf

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the clever-f.vim port (https://github.com/rhysd/clever-f.vim).
 *
 * Expected behaviour follows the original plugin's defaults:
 * - `f`/`F`/`t`/`T` repeat the last search when pressed again at the position the previous search left the cursor at
 * - the direction is relative to the original search (`f` continues it, `F` reverses it)
 * - searches cross line boundaries (`g:clever_f_across_no_line = 0`)
 * - any other cursor movement, text change, or entering Insert mode resets the state
 */
class CleverFExtensionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("clever-f")
  }

  // f

  @Test
  fun `test f finds char`() {
    doTest("fa", "${c}one a two a three a", "one ${c}a two a three a")
  }

  @Test
  fun `test f repeated finds next char`() {
    doTest("faf", "${c}one a two a three a", "one a two ${c}a three a")
  }

  @Test
  fun `test f repeated twice finds third char`() {
    doTest("faff", "${c}one a two a three a", "one a two a three ${c}a")
  }

  @Test
  fun `test F after f goes back`() {
    doTest("fafF", "${c}one a two a three a", "one ${c}a two a three a")
  }

  @Test
  fun `test f repeated stays on last match when there are no more matches`() {
    doTest("faff", "${c}xa a", "xa ${c}a")
  }

  @Test
  fun `test f with no match does not move`() {
    doTest("fz", "${c}one a two", "${c}one a two")
  }

  // F

  @Test
  fun `test F finds char backward`() {
    doTest("Fa", "a one a two a thre${c}e", "a one a two ${c}a three")
  }

  @Test
  fun `test f after F continues backward`() {
    doTest("Faf", "a one a two a thre${c}e", "a one ${c}a two a three")
  }

  @Test
  fun `test F after F goes forward`() {
    doTest("FafF", "a one a two a thre${c}e", "a one a two ${c}a three")
  }

  // t / T

  @Test
  fun `test t stops before char`() {
    doTest("ta", "${c}xx a yy a zz", "xx${c} a yy a zz")
  }

  @Test
  fun `test t repeated does not get stuck before adjacent char`() {
    doTest("tat", "${c}xx a yy a zz", "xx a yy${c} a zz")
  }

  @Test
  fun `test T after t goes back`() {
    doTest("tatT", "${c}xx a yy a zz", "xx a${c} yy a zz")
  }

  @Test
  fun `test T stops after char`() {
    doTest("Ta", "zz a yy a x${c}x", "zz a yy a${c} xx")
  }

  @Test
  fun `test f after T continues backward without getting stuck after adjacent char`() {
    doTest("Taf", "zz a yy a x${c}x", "zz a${c} yy a xx")
  }

  // Count

  @Test
  fun `test f with count`() {
    doTest("2fa", "${c}-a-a-a-a", "-a-${c}a-a-a")
  }

  @Test
  fun `test f with count then repeat`() {
    doTest("2faf", "${c}-a-a-a-a", "-a-a-${c}a-a")
  }

  @Test
  fun `test repeat with count`() {
    doTest("fa2f", "${c}-a-a-a-a", "-a-a-${c}a-a")
  }

  // Multiline

  @Test
  fun `test f crosses lines by default`() {
    val before = """
      |${c}one
      |two a
    """.trimMargin()
    val after = """
      |one
      |two ${c}a
    """.trimMargin()
    doTest("fa", before, after)
  }

  @Test
  fun `test f repeated crosses lines`() {
    val before = """
      |${c}one a
      |two a
    """.trimMargin()
    val after = """
      |one a
      |two ${c}a
    """.trimMargin()
    doTest("faf", before, after)
  }

  @Test
  fun `test F crosses lines by default`() {
    val before = """
      |a one
      |tw${c}o
    """.trimMargin()
    val after = """
      |${c}a one
      |two
    """.trimMargin()
    doTest("Fa", before, after)
  }

  @Test
  fun `test f does not cross lines with clever_f_across_no_line`() {
    val before = """
      |${c}one
      |two a
    """.trimMargin()
    doTest(
      "fa",
      before,
      before,
      afterEditorInitialized = { enterCommand("let g:clever_f_across_no_line = 1") },
    )
  }

  @Test
  fun `test f repeated does not cross lines with clever_f_across_no_line`() {
    val before = """
      |${c}xa
      |a
    """.trimMargin()
    val after = """
      |x${c}a
      |a
    """.trimMargin()
    doTest(
      "faf",
      before,
      after,
      afterEditorInitialized = { enterCommand("let g:clever_f_across_no_line = 1") },
    )
  }

  // Reset

  @Test
  fun `test other motion resets search`() {
    // After `l` the cursor is not where the last search left it, so `f` asks for a new char
    doTest("falfb", "${c}xa b a b", "xa ${c}b a b")
  }

  @Test
  fun `test text change resets search`() {
    // `x` leaves the cursor in place, but the buffer changed, so `f` asks for a new char
    doTest("faxfc", "${c}-a c a c", "- ${c}c a c")
  }

  @Test
  fun `test entering insert mode resets search`() {
    // `a<Esc>` returns the cursor to the same position, but entering Insert mode resets the state
    doTest("faa<Esc>fc", "${c}-a c a c", "-a ${c}c a c")
  }

  @Test
  fun `test reset mapping resets search`() {
    doTest(
      "faQfc",
      "${c}-a c a c",
      "-a ${c}c a c",
      afterEditorInitialized = { enterCommand("nmap Q <Plug>(clever-f-reset)") },
    )
  }

  @Test
  fun `test macro execution always asks for char`() {
    // Replaying `fa` from the position the recording left the cursor at must not turn into a repeat
    doTest("qqfaq@q", "${c}-a-a-a-a", "-a-${c}a-a-a")
  }

  // Visual mode

  @Test
  fun `test f in visual mode extends selection`() {
    doTest(
      "vfaf",
      "${c}xa b a",
      "${s}xa b ${c}a${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test visual mode does not reuse normal mode search`() {
    doTest(
      "favfc",
      "${c}-a c a c",
      "-${s}a ${c}c${se} a c",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // Operator-pending mode

  @Test
  fun `test df deletes inclusive`() {
    doTest("dfa", "${c}one a two a", "${c} two a")
  }

  @Test
  fun `test dt deletes exclusive`() {
    doTest("dta", "${c}one a two", "${c}a two")
  }

  @Test
  fun `test dF deletes backward exclusive of cursor`() {
    doTest("dFa", "one a tw${c}o", "one ${c}o")
  }

  @Test
  fun `test df crosses lines`() {
    val before = """
      |${c}one
      |two a three
    """.trimMargin()
    doTest("dfa", before, "${c} three")
  }

  // Case sensitivity

  @Test
  fun `test f is case sensitive by default`() {
    doTest("fa", "${c}xA a", "xA ${c}a")
  }

  @Test
  fun `test f with clever_f_ignore_case`() {
    doTest(
      "fa",
      "${c}xA a",
      "x${c}A a",
      afterEditorInitialized = { enterCommand("let g:clever_f_ignore_case = 1") },
    )
  }

  @Test
  fun `test f lowercase with clever_f_smart_case matches both cases`() {
    doTest(
      "faf",
      "${c}xa A",
      "xa ${c}A",
      afterEditorInitialized = { enterCommand("let g:clever_f_smart_case = 1") },
    )
  }

  @Test
  fun `test f uppercase with clever_f_smart_case matches only uppercase`() {
    doTest(
      "fA",
      "${c}xa a A",
      "xa a ${c}A",
      afterEditorInitialized = { enterCommand("let g:clever_f_smart_case = 1") },
    )
  }

  // g:clever_f_fix_key_direction

  @Test
  fun `test F after F goes backward with clever_f_fix_key_direction`() {
    doTest(
      "FaF",
      "a one a two a thre${c}e",
      "a one ${c}a two a three",
      afterEditorInitialized = { enterCommand("let g:clever_f_fix_key_direction = 1") },
    )
  }

  @Test
  fun `test f after F goes forward with clever_f_fix_key_direction`() {
    doTest(
      "FaFf",
      "a one a two a thre${c}e",
      "a one a two ${c}a three",
      afterEditorInitialized = { enterCommand("let g:clever_f_fix_key_direction = 1") },
    )
  }

  // g:clever_f_repeat_last_char_inputs

  @Test
  fun `test f with enter reuses last char`() {
    doTest("fa0f<CR>", "${c}-a-a", "-${c}a-a")
  }

  @Test
  fun `test f with enter and no previous char does not move`() {
    doTest("f<CR>", "${c}-a-a", "${c}-a-a")
  }

  // g:clever_f_chars_match_any_signs

  @Test
  fun `test f with sign char matches any sign`() {
    doTest(
      "f;",
      "${c}abc,def.g",
      "abc${c},def.g",
      afterEditorInitialized = { enterCommand("let g:clever_f_chars_match_any_signs = ';'") },
    )
  }

  @Test
  fun `test f with sign char repeated matches next sign`() {
    doTest(
      "f;f",
      "${c}abc,def.g",
      "abc,def${c}.g",
      afterEditorInitialized = { enterCommand("let g:clever_f_chars_match_any_signs = ';'") },
    )
  }

  // g:clever_f_not_overwrites_standard_mappings

  @Test
  fun `test standard f is kept with clever_f_not_overwrites_standard_mappings`() {
    // With the standard `f`, `fafa` is two separate `fa` searches
    doTest(
      "fafa",
      "${c}-a-a",
      "-a-${c}a",
      afterEditorInitialized = {
        enterCommand("set noclever-f")
        enterCommand("let g:clever_f_not_overwrites_standard_mappings = 1")
        enterCommand("set clever-f")
      },
    )
  }

  // <Plug> mappings

  @Test
  fun `test plug mappings for f`() {
    doTest(
      "sas",
      "${c}-a-a",
      "-a-${c}a",
      afterEditorInitialized = {
        enterCommand("set noclever-f")
        enterCommand("let g:clever_f_not_overwrites_standard_mappings = 1")
        enterCommand("set clever-f")
        enterCommand("nmap s <Plug>(clever-f-f)")
      },
    )
  }

  @Test
  fun `test repeat forward mapping`() {
    doTest(
      "fa;",
      "${c}-a-a",
      "-a-${c}a",
      afterEditorInitialized = { enterCommand("nmap ; <Plug>(clever-f-repeat-forward)") },
    )
  }

  @Test
  fun `test repeat back mapping`() {
    doTest(
      "faf,",
      "${c}-a-a",
      "-${c}a-a",
      afterEditorInitialized = { enterCommand("nmap , <Plug>(clever-f-repeat-back)") },
    )
  }
}

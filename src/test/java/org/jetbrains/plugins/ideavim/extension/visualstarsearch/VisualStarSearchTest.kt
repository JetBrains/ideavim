/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualstarsearch

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the vim-visual-star-search extension.
 *
 * See https://github.com/bronson/vim-visual-star-search
 *
 * The plugin makes `*` and `#` work in Visual mode: instead of searching for the word under the caret, they search
 * forwards/backwards for the selected text, taken literally (no `\<`/`\>` word boundaries, no magic characters).
 */
class VisualStarSearchTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("visual-star-search")
  }

  // ------------------------------------------------------------------------------------------------ core: * and #

  @Test
  fun `test star searches forwards for the selected text`() {
    doTest(
      "ve*",
      """
        ${c}hello world
        hello world
      """.trimIndent(),
      """
        hello world
        ${c}hello world
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test hash searches backwards for the selected text`() {
    doTest(
      "ve#",
      """
        hello world
        ${c}hello world
      """.trimIndent(),
      """
        ${c}hello world
        hello world
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star matches text without word boundaries`() {
    doTest(
      "ve*",
      """
        ${c}foo bar
        foobar baz
        foo end
      """.trimIndent(),
      """
        foo bar
        ${c}foobar baz
        foo end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star matches part of a word`() {
    doTest(
      "lvl*",
      """
        ${c}foo
        xoo
        foo
      """.trimIndent(),
      """
        foo
        x${c}oo
        foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star searches for selection containing spaces`() {
    doTest(
      "v6l*",
      """
        ${c}foo bar baz
        foo baz
        foo bar end
      """.trimIndent(),
      """
        foo bar baz
        foo baz
        ${c}foo bar end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star searches multiline selection`() {
    doTest(
      "vjll*",
      """
        ${c}foo
        bar
        baz
        foo
        bar
      """.trimIndent(),
      """
        foo
        bar
        baz
        ${c}foo
        bar
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star wraps around the end of the file`() {
    doTest(
      "ve*",
      """
        foo bar
        ${c}foo end
      """.trimIndent(),
      """
        ${c}foo bar
        foo end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test hash wraps around the start of the file`() {
    doTest(
      "ve#",
      """
        ${c}foo bar
        foo end
      """.trimIndent(),
      """
        foo bar
        ${c}foo end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // ------------------------------------------------------------------------------- the search pattern is remembered

  @Test
  fun `test n repeats the visual star search`() {
    doTest(
      "ve*n",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        foo bar
        foobar
        ${c}foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test N reverses the visual star search`() {
    doTest(
      "ve*nN",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        foo bar
        ${c}foobar
        foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test n continues backwards after hash`() {
    doTest(
      "ve#n",
      """
        foo one
        foo two
        ${c}foo three
      """.trimIndent(),
      """
        ${c}foo one
        foo two
        foo three
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // -------------------------------------------------------------------- the selection is searched for literally

  @Test
  fun `test dot in selection is taken literally`() {
    doTest(
      "vll*",
      """
        ${c}a.b
        axb
        a.b
      """.trimIndent(),
      """
        a.b
        axb
        ${c}a.b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star in selection is taken literally`() {
    doTest(
      "vll*",
      """
        ${c}a*b
        aab
        a*b
      """.trimIndent(),
      """
        a*b
        aab
        ${c}a*b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test square bracket in selection is taken literally`() {
    doTest(
      "v3l*",
      """
        ${c}x[0] = 1
        x[i] = 2
        x[0] = 3
      """.trimIndent(),
      """
        x[0] = 1
        x[i] = 2
        ${c}x[0] = 3
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test tilde in selection is taken literally`() {
    doTest(
      "vll*",
      """
        ${c}a~b
        axb
        a~b
      """.trimIndent(),
      """
        a~b
        axb
        ${c}a~b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test backslash in selection is taken literally`() {
    doTest(
      "vll*",
      """
        ${c}a\b
        axb
        a\b
      """.trimIndent(),
      """
        a\b
        axb
        ${c}a\b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test slash in selection does not break the forward search`() {
    doTest(
      "vll*",
      """
        ${c}a/b
        axb
        a/b
      """.trimIndent(),
      """
        a/b
        axb
        ${c}a/b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test question mark in selection does not break the backward search`() {
    doTest(
      "vll#",
      """
        a?b
        axb
        ${c}a?b
      """.trimIndent(),
      """
        ${c}a?b
        axb
        a?b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // ----------------------------------------------------------------------------------------------- no side effects

  @Test
  fun `test normal mode star still searches for the whole word`() {
    doTest(
      "*",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        foo bar
        foobar
        ${c}foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test unnamed register is not clobbered`() {
    doTest(
      "yiwve*P",
      """
        ${c}foo bar
        foo baz
      """.trimIndent(),
      """
        foo bar
        fo${c}ofoo baz
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // ------------------------------------------------------------------- linewise selection (`V`)

  // The plugin yanks the selection with `gvy`, so a linewise selection yields the whole line plus its trailing new
  // line character. The pattern therefore only matches text that ends a line - the mid-line "foo" is skipped.
  @Test
  fun `test V searches for the whole line including the trailing new line`() {
    doTest(
      "V*",
      """
        ${c}foo
        foo bar
        foo
        end
      """.trimIndent(),
      """
        foo
        foo bar
        ${c}foo
        end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test V includes the line indent in the pattern`() {
    doTest(
      "V*",
      """
        ${c}..foo
        foo
        ..foo
        end
      """.trimIndent().dotToSpace(),
      """
        ..foo
        foo
        ${c}..foo
        end
      """.trimIndent().dotToSpace(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test V over two lines searches for both lines`() {
    doTest(
      "Vj*",
      """
        ${c}aa
        bb
        cc
        aa
        bb
      """.trimIndent(),
      """
        aa
        bb
        cc
        ${c}aa
        bb
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test hash searches backwards for the selected line`() {
    doTest(
      "V#",
      """
        foo
        bar
        ${c}foo
        end
      """.trimIndent(),
      """
        ${c}foo
        bar
        foo
        end
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test n repeats the linewise search`() {
    doTest(
      "V*n",
      """
        ${c}foo
        x
        foo
        y
        foo
      """.trimIndent(),
      """
        foo
        x
        foo
        y
        ${c}foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // The last line has no trailing new line character in the document, but Vim still matches it
  @Test
  fun `test V matches the last line of the file`() {
    doTest(
      "V*",
      """
        ${c}foo
        bar
        foo
      """.trimIndent(),
      """
        foo
        bar
        ${c}foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test V does not move the caret when the selected line is the only match`() {
    doTest(
      "V*",
      """
        ${c}foo
        bar
        baz
      """.trimIndent(),
      """
        ${c}foo
        bar
        baz
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // ------------------------------------------------------------------ blockwise selection (`<C-V>`)

  @Test
  fun `test blockwise selection of a single line behaves like a charwise selection`() {
    doTest(
      "l<C-V>ll*",
      """
        ${c}xfoo
        yfoo
        zfoo
      """.trimIndent(),
      """
        xfoo
        y${c}foo
        zfoo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // `gvy` on a blockwise selection yields the block rows joined by new line characters, so the pattern matches where
  // one line ends with the first row and the next line starts with the second row. Text outside the block (the "y" of
  // "yar") is not part of the pattern.
  @Test
  fun `test blockwise selection joins the block rows with new lines`() {
    doTest(
      "l<C-V>jl*",
      """
        ${c}xoo
        yar
        zoo
        arq
      """.trimIndent(),
      """
        xoo
        yar
        z${c}oo
        arq
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // -------------------------------------------------------------------------- 'ignorecase' and 'smartcase'

  // The plugin searches with a plain `/`, without a `\c` or `\C` prefix, so the search honours 'ignorecase'. This is
  // unlike the built-in `*`, which always searches case sensitively.
  @Test
  fun `test ignorecase makes the search case insensitive`() {
    configureByText(
      """
        ${c}foo
        FOO
        end
      """.trimIndent(),
    )
    enterCommand("set ignorecase")
    typeText("ve*")
    assertState(
      """
        foo
        ${c}FOO
        end
      """.trimIndent(),
    )
    assertState(Mode.NORMAL())
  }

  @Test
  fun `test search is case sensitive without ignorecase`() {
    configureByText(
      """
        ${c}foo
        FOO
        end
      """.trimIndent(),
    )
    enterCommand("set noignorecase")
    typeText("ve*")
    assertState(
      """
        ${c}foo
        FOO
        end
      """.trimIndent(),
    )
    assertState(Mode.NORMAL())
  }

  // The pattern is "typed" into the search command, so 'smartcase' applies too. Again unlike the built-in `*`, which
  // never uses 'smartcase'.
  @Test
  fun `test smartcase makes an upper case selection case sensitive`() {
    configureByText(
      """
        ${c}Foo
        foo
        Foo
        end
      """.trimIndent(),
    )
    enterCommand("set ignorecase smartcase")
    typeText("ve*")
    assertState(
      """
        Foo
        foo
        ${c}Foo
        end
      """.trimIndent(),
    )
    assertState(Mode.NORMAL())
  }

  @Test
  fun `test smartcase does not affect a lower case selection`() {
    configureByText(
      """
        ${c}foo
        FOO
        end
      """.trimIndent(),
    )
    enterCommand("set ignorecase smartcase")
    typeText("ve*")
    assertState(
      """
        foo
        ${c}FOO
        end
      """.trimIndent(),
    )
    assertState(Mode.NORMAL())
  }

  @Test
  fun `test ignorecase applies to the backward search`() {
    configureByText(
      """
        FOO
        bar
        ${c}foo
        end
      """.trimIndent(),
    )
    enterCommand("set ignorecase")
    typeText("ve#")
    assertState(
      """
        ${c}FOO
        bar
        foo
        end
      """.trimIndent(),
    )
    assertState(Mode.NORMAL())
  }

  @Test
  fun `test ignorecase applies to n`() {
    configureByText(
      """
        ${c}foo
        FOO
        x
        FOo
      """.trimIndent(),
    )
    enterCommand("set ignorecase")
    typeText("ve*n")
    assertState(
      """
        foo
        FOO
        x
        ${c}FOo
      """.trimIndent(),
    )
    assertState(Mode.NORMAL())
  }

  // The delimiter is escaped as a decimal character code, and that code is greedy, so a digit directly after the
  // delimiter must not be swallowed by the number
  @Test
  fun `test question mark followed by a digit does not break the backward search`() {
    doTest(
      "v3l#",
      """
        a?7b
        ax7b
        ${c}a?7b
      """.trimIndent(),
      """
        ${c}a?7b
        ax7b
        a?7b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test slash followed by a digit does not break the forward search`() {
    doTest(
      "v3l*",
      """
        ${c}a/7b
        ax7b
        a/7b
      """.trimIndent(),
      """
        a/7b
        ax7b
        ${c}a/7b
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // --------------------------------------------------------------------------------------------------- jump list

  // The plugin searches with `/`, which is a jump command, so the position before the search is added to the jump
  // list. Note that the saved position is the start of the selection - the plugin yanks with `gvy`, which leaves the
  // caret there - and not the caret position inside the visual selection
  @Test
  fun `test star adds the position before the search to the jump list`() {
    doTest(
      "ve*<C-O>",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test hash adds the position before the search to the jump list`() {
    doTest(
      "ve#<C-O>",
      """
        foo
        bar
        ${c}foo
      """.trimIndent(),
      """
        foo
        bar
        ${c}foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test jump back and forward returns to the search result`() {
    doTest(
      "ve*<C-O><C-I>",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        foo bar
        ${c}foobar
        foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test star sets the previous context mark`() {
    doTest(
      "ve*''",
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      """
        ${c}foo bar
        foobar
        foo
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}

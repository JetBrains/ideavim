/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.abolish

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.action.ex.VimExTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Behaviour spec for the `:Abolish` command, modelled on tpope/vim-abolish
 * (`plugin/abolish.vim`). `:Abolish` dispatches to four sub-commands based on
 * the first `-flag`:
 *
 *  - *(default)* — create case-aware abbreviations (`inoreabbrev`/`noreabbrev`)
 *  - `-delete`   — remove abbreviations
 *  - `-search`   — search across case variants (like `:S`/`:Subvert` search)
 *  - `-substitute` — case-aware `:substitute`
 *
 * Every variant multiplies the word into three case forms — lowercase,
 * PascalCase, UPPERCASE — exactly like [buildVariantDictionary], and brace
 * pairs (`{a,b}`) expand into alternatives.
 *
 * Abbreviation expansion is driven the same way as [AbbrevCommandTest]: enter
 * insert mode, type the trigger word followed by a non-keyword char (space),
 * and observe the buffer.
 *
 * NOTE: `:Abolish!` in abbreviation mode persists the entry to
 * `g:abolish_save_file` (writes to disk). That side-effecting behaviour is
 * intentionally NOT covered here — these tests stay user-facing and avoid
 * touching the filesystem. The `!` is still exercised in its other meanings:
 * backward search (`-search`) and current-line-only (`-substitute`).
 */
class AbolishCommandTest : VimExTestCase() {

  // :Abolish creates real abbreviations, which are global JVM state and leak
  // across tests. Mirror AbbrevCommandTest: defensively isolate on entry and
  // clean up on exit (leftover ex-entry state can otherwise suppress abclear).
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("abolish")
    resetAbbreviations()
  }

  @AfterEach
  fun tearDownAbbreviations() = resetAbbreviations()

  private fun resetAbbreviations() {
    deactivateExEntry()
    enterCommand("abclear")
  }

  // ---------------------------------------------------------------------------
  // Abbreviation mode (default)
  // ---------------------------------------------------------------------------

  @Test
  fun `Abolish expands the lowercase Pascal and uppercase variants of a word`() {
    configureByText("${c}\n")
    enterCommand("Abolish teh the")
    typeText("i", "teh Teh TEH ")
    assertState("the The THE \n")
  }

  @Test
  fun `Abolish with brace alternatives expands singular and plural forms`() {
    configureByText("${c}\n")
    enterCommand("Abolish anomol{y,ies} anomal{}")
    typeText("i", "anomoly anomolies ")
    assertState("anomaly anomalies \n")
  }

  @Test
  fun `Abolish with prefix and suffix braces and empty slots`() {
    configureByText("${c}\n")
    enterCommand("Abolish {,in}consistant{,ly} {}consistent{}")
    typeText("i", "consistant inconsistantly ")
    assertState("consistent inconsistently \n")
  }

  @Test
  fun `Abolish brace alternatives also expand their case variants`() {
    configureByText("${c}\n")
    enterCommand("Abolish anomol{y,ies} anomal{}")
    typeText("i", "Anomoly ANOMOLIES ")
    assertState("Anomaly ANOMALIES \n")
  }

  @Test
  fun `Abolish cycles rhs brace alternatives when lhs has more of them`() {
    configureByText("${c}\n")
    enterCommand("Abolish foo{a,b,c,d} bar{x,y}")
    typeText("i", "fooa foob fooc food ")
    assertState("barx bary barx bary \n")
  }

  @Test
  fun `Abolish supports a multi-word replacement`() {
    configureByText("${c}\n")
    enterCommand("Abolish Tqbf The quick, brown fox")
    typeText("i", "Tqbf ")
    assertState("The quick, brown fox \n")
  }

  @Test
  fun `Abolish abbreviation only expands a whole word, not a substring`() {
    configureByText("${c}\n")
    enterCommand("Abolish foo bar")
    typeText("i", "xfoo ")
    assertState("xfoo \n")
  }

  @Test
  fun `Abolish does not expand a case form that is not one of the three variants`() {
    configureByText("${c}\n")
    enterCommand("Abolish foo bar")
    typeText("i", "fOo ")
    assertState("fOo \n")
  }

  @Test
  fun `Abolish honours iskeyword when the lhs contains a dash`() {
    configureByText("${c}\n")
    enterCommand("set iskeyword+=-")
    enterCommand("Abolish foo-bar baz")
    assertPluginError(false)
    typeText("i", "foo-bar ")
    assertState("baz \n")
  }

  // ---------------------------------------------------------------------------
  // -delete
  // ---------------------------------------------------------------------------

  @Test
  fun `Abolish -delete removes every case variant of an abbreviation`() {
    configureByText("${c}\n")
    enterCommand("Abolish foo bar")
    enterCommand("Abolish -delete foo")
    typeText("i", "foo Foo FOO ")
    assertState("foo Foo FOO \n")
  }

  @Test
  fun `Abolish -delete with braces removes all the expanded variants`() {
    configureByText("${c}\n")
    enterCommand("Abolish anomol{y,ies} anomal{}")
    enterCommand("Abolish -delete anomol{y,ies}")
    typeText("i", "anomoly anomolies ")
    assertState("anomoly anomolies \n")
  }

  @Test
  fun `Abolish -delete of an undefined abbreviation is silent`() {
    configureByText("${c}\n")
    enterCommand("Abolish -delete neverdefined")
    assertPluginError(false)
    // The extension is still usable afterwards.
    enterCommand("Abolish foo bar")
    typeText("i", "foo ")
    assertState("bar \n")
  }

  // ---------------------------------------------------------------------------
  // -buffer / -cmdline modifiers
  // ---------------------------------------------------------------------------

  @Test
  fun `Abolish -buffer expands in the current buffer`() {
    configureByText("${c}\n")
    enterCommand("Abolish -buffer foo bar")
    typeText("i", "foo ")
    assertState("bar \n")
  }

  @Test
  fun `Abolish -buffer does not expand in a different buffer`() {
    configureByText("${c}\n")
    enterCommand("Abolish -buffer foo bar")
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile("other.txt", "\n"))
    }
    typeText("0")
    typeText("i", "foo ")
    assertState("foo \n")
  }

  @Test
  fun `Abolish -cmdline expands the abbreviation in command-line mode`() {
    enterCommand("Abolish -cmdline foo bar")
    typeText(":foo ")
    assertExText("bar ")
  }

  @Test
  fun `Abolish without -cmdline does not expand in command-line mode`() {
    enterCommand("Abolish foo bar")
    typeText(":foo ")
    assertExText("foo ")
  }

  // ---------------------------------------------------------------------------
  // -search
  // ---------------------------------------------------------------------------

  @Test
  fun `Abolish -search jumps forward to the first matching case variant`() {
    doTest(
      ":Abolish -search foo<CR>",
      "${c}begin FOO middle Foo end foo",
      "begin ${c}FOO middle Foo end foo",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `Abolish bang -search jumps backward to the previous matching variant`() {
    doTest(
      ":Abolish! -search foo<CR>",
      "begin Foo middle FOO end ${c}some here",
      "begin Foo middle ${c}FOO end some here",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `Abolish -search expands brace alternatives across case variants`() {
    doTest(
      ":Abolish -search box{,es}<CR>",
      "${c}start word Boxes here box there",
      "start word ${c}Boxes here box there",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `n navigates to the next match after an Abolish -search`() {
    doTest(
      listOf(":Abolish -search foo<CR>", "n"),
      "${c}begin FOO middle Foo end foo",
      "begin FOO middle ${c}Foo end foo",
      Mode.NORMAL(),
    )
  }

  // ---------------------------------------------------------------------------
  // -substitute
  //
  // tpope quirk (s:commands.substitute.process): the range is ignored; the
  // command operates on the whole file (`%`) by default, or only the current
  // line (`.`) when banged. The `g` flag is on by default.
  // ---------------------------------------------------------------------------

  @Test
  fun `Abolish -substitute replaces all case variants on the line with g implied`() {
    doTest(
      ":%Abolish -substitute foo bar<CR>",
      "${c}foo Foo FOO foo",
      "${c}bar Bar BAR bar",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `Abolish -substitute with braces pairs singular and plural variants`() {
    doTest(
      ":%Abolish -substitute box{,es} bag{,s}<CR>",
      "${c}box Box BOX boxes Boxes BOXES",
      "${c}bag Bag BAG bags Bags BAGS",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `Abolish -substitute without a range defaults to the whole file`() {
    doTest(
      ":Abolish -substitute foo bar<CR>",
      "${c}foo\nfoo",
      "bar\n${c}bar",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `Abolish bang -substitute changes only the current line`() {
    doTest(
      ":Abolish! -substitute foo bar<CR>",
      "${c}foo\nfoo",
      "${c}bar\nfoo",
      Mode.NORMAL(),
    )
  }

  // ---------------------------------------------------------------------------
  // Malformed input — must not corrupt the extension or define junk
  // ---------------------------------------------------------------------------

  @Test
  fun `Abolish with no arguments does not break the extension`() {
    configureByText("${c}\n")
    enterCommand("Abolish")
    // A subsequent well-formed command still works.
    enterCommand("Abolish foo bar")
    typeText("i", "foo ")
    assertState("bar \n")
  }

  @Test
  fun `Abolish with an unknown option does not define an abbreviation`() {
    configureByText("${c}\n")
    enterCommand("Abolish -nope foo bar")
    typeText("i", "foo ")
    assertState("foo \n")
  }
}

/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.camelcasemotion

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Behavior specs for the CamelCaseMotion extension (VIM-1984).
 *
 * They encode the intended Vim behavior, using `,` as the motion key (`g:camelcasemotion_key = ','`)
 * to keep the typed keys free of backslash escaping.
 */
class CamelCaseMotionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    // The variable must be set before the extension is enabled: init() reads it to build mappings.
    configureByText("")
    enterCommand("let g:camelcasemotion_key = ','")
    enableExtensions("CamelCaseMotion")
  }

  @Test
  fun `test forward moves to next CamelCase boundary`() {
    doTest(",w", "${c}CamelCaseWord", "Camel${c}CaseWord", Mode.NORMAL())
  }

  @Test
  fun `test forward with count skips multiple boundaries`() {
    doTest("2,w", "${c}CamelCaseWord", "CamelCase${c}Word", Mode.NORMAL())
  }

  @Test
  fun `test forward moves across underscore boundary`() {
    doTest(",w", "${c}my_camel_case", "my_${c}camel_case", Mode.NORMAL())
  }

  @Test
  fun `test backward moves to previous CamelCase boundary`() {
    doTest(",b", "CamelCase${c}Word", "Camel${c}CaseWord", Mode.NORMAL())
  }

  @Test
  fun `test end moves to last char of current word`() {
    doTest(",e", "${c}CamelCaseWord", "Came${c}lCaseWord", Mode.NORMAL())
  }

  @Test
  fun `test delete inner word removes the CamelCase chunk under caret`() {
    doTest("di,w", "Camel${c}CaseWord", "Camel${c}Word", Mode.NORMAL())
  }

  @Test
  fun `test inner word selects the chunk in visual mode`() {
    doTest("vi,w", "Camel${c}CaseWord", "Camel${s}Cas${c}e${se}Word", Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  // ---- iw vs ie: trailing delimiter -----------------------------------------------------------
  // Upstream `iw` extends to the start of the next "word" and so includes the trailing delimiter;
  // `ie` stops at the end of the current "word" and excludes it. (camelcasemotion#InnerMotion.)

  @Test
  fun `test inner word includes the trailing underscore`() {
    doTest("di,w", "my_c${c}amel_case", "my_${c}case", Mode.NORMAL())
  }

  @Test
  fun `test inner end excludes the trailing underscore`() {
    doTest("di,e", "my_c${c}amel_case", "my_${c}_case", Mode.NORMAL())
  }

  // ---- counts on inner objects ----------------------------------------------------------------

  @Test
  fun `test inner word with count spans words and trailing delimiter`() {
    doTest("d2i,w", "${c}foo_bar_baz", "${c}baz", Mode.NORMAL())
  }

  @Test
  fun `test inner end with count spans words without trailing delimiter`() {
    doTest("d2i,e", "${c}foo_bar_baz", "${c}_baz", Mode.NORMAL())
  }

  // ---- ib extends backward --------------------------------------------------------------------
  // Upstream decrees `ib` is the opposite of `ie` (b then e), so a count selects the current chunk
  // plus preceding ones. Cursor is in `baz`; 2ib covers `bar_baz`.

  @Test
  fun `test inner back with count extends backward`() {
    doTest("d2i,b", "foo_bar_b${c}az_qux", "foo_${c}_qux", Mode.NORMAL())
  }

  // ---- ge: backward to end of previous word ---------------------------------------------------

  @Test
  fun `test ge moves to end of previous word`() {
    doTest(",ge", "CamelCase${c}Word", "CamelCas${c}eWord", Mode.NORMAL())
  }

  @Test
  fun `test end with count moves over multiple words`() {
    doTest("2,e", "${c}CamelCaseWord", "CamelCas${c}eWord", Mode.NORMAL())
  }

  // ---- e / ge are inclusive under operators ---------------------------------------------------
  // Like Vim's built-in `e`/`ge`, the forward/backward-to-end motions must INCLUDE the character
  // they land on when used with an operator. A plain exclusive caret move deletes one char short.

  @Test
  fun `test end selects through the landing char in visual mode`() {
    doTest("v,e", "${c}CamelCaseWord", "${s}Came${c}l${se}CaseWord", Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test delete to end is inclusive`() {
    doTest("d,e", "${c}CamelCaseWord", "${c}CaseWord", Mode.NORMAL())
  }

  @Test
  fun `test delete to end with count is inclusive`() {
    doTest("d2,e", "${c}CamelCaseWord", "${c}Word", Mode.NORMAL())
  }

  @Test
  fun `test delete to previous end is inclusive`() {
    doTest("d,ge", "CamelCase${c}Word", "CamelCas${c}ord", Mode.NORMAL())
  }

  // ---- boundary detection: ALLCAPS, digits, mixed ---------------------------------------------
  // Pins the trickier branches of the boundary scanner that the earlier tests didn't exercise.

  @Test
  fun `test forward splits ALLCAPS run before CamelCase`() {
    // HTTPServer -> HTTP | Server: the last capital that starts a lowercase word is the boundary.
    doTest(",w", "${c}HTTPServer", "HTTP${c}Server", Mode.NORMAL())
  }

  @Test
  fun `test end stops at the last char of the ALLCAPS run`() {
    doTest(",e", "${c}HTTPServer", "HTT${c}PServer", Mode.NORMAL())
  }

  @Test
  fun `test forward with count over ALLCAPS and CamelCase`() {
    doTest("2,w", "${c}HTTPServerName", "HTTPServer${c}Name", Mode.NORMAL())
  }

  @Test
  fun `test forward stops at letter to digit boundary`() {
    doTest(",w", "${c}abc123", "abc${c}123", Mode.NORMAL())
  }

  @Test
  fun `test forward stops at digit to letter boundary`() {
    doTest(",w", "${c}123abc", "123${c}abc", Mode.NORMAL())
  }

  @Test
  fun `test forward keeps a multi-digit run together`() {
    // Regression guard: 1234Test must move as [1]234[T]est, not stop at the second digit.
    doTest(",w", "${c}1234Test", "1234${c}Test", Mode.NORMAL())
  }

  @Test
  fun `test forward stops at ALLCAPS to digit boundary`() {
    doTest(",w", "${c}ABC123", "ABC${c}123", Mode.NORMAL())
  }

  @Test
  fun `test forward handles mixed CamelCase and underscore`() {
    doTest(",w", "${c}fooBar_baz", "foo${c}Bar_baz", Mode.NORMAL())
  }

  // ---- ige: inner backward-to-end -------------------------------------------------------------
  // Verified against the reference plugin: `ige` selects from one char before the count-th previous
  // word-end through the current word's start (it crosses the boundary into the previous word).

  @Test
  fun `test inner backward-end selects across the previous CamelCase boundary`() {
    doTest("di,ge", "CamelC${c}aseWord", "Cam${c}aseWord", Mode.NORMAL())
  }

  @Test
  fun `test inner backward-end crosses an underscore boundary`() {
    doTest("di,ge", "my_ca${c}mel_case", "${c}amel_case", Mode.NORMAL())
  }

  @Test
  fun `test inner backward-end with count spans multiple previous words`() {
    doTest("d2i,ge", "foo_bar_b${c}az", "f${c}az", Mode.NORMAL())
  }

  @Test
  fun `test inner backward-end at the first word selects only that char`() {
    doTest("di,ge", "${c}foo_bar_baz", "${c}oo_bar_baz", Mode.NORMAL())
  }

  // ---- iskeyword: characters added to 'iskeyword' count as word chars -------------------------
  // By default `$` is punctuation, so `foo$bar` is two words. After `set iskeyword+=$` it becomes a
  // word character and `foo$bar` is a single "word" for boundary purposes.

  @Test
  fun `test end stops before punctuation by default`() {
    doTest(",e", "${c}foo\$bar", "fo${c}o\$bar", Mode.NORMAL())
  }

  @Test
  fun `test end treats iskeyword punctuation as part of the word`() {
    doTest(
      ",e", "${c}foo\$bar", "foo\$ba${c}r", Mode.NORMAL(),
      afterEditorInitialized = { enterCommand("set iskeyword+=$") },
    )
  }

  @Test
  fun `test inner end respects iskeyword`() {
    doTest(
      "di,e", "f${c}oo\$bar.x", "${c}.x", Mode.NORMAL(),
      afterEditorInitialized = { enterCommand("set iskeyword+=$") },
    )
  }


  @Test
  fun `test forward stops at inline punctuation`() {
    // Upstream skips the dot (foo -> bar); regular Vim w stops on it, and so do we.
    doTest(",w", "${c}foo.bar", "foo${c}.bar", Mode.NORMAL())
  }

  @Test
  fun `test end stops before punctuation`() {
    doTest(",e", "${c}foo.bar", "fo${c}o.bar", Mode.NORMAL())
  }

  @Test
  fun `test forward stops at a bracket`() {
    doTest(",w", "${c}foo(bar)", "foo${c}(bar)", Mode.NORMAL())
  }

  @Test
  fun `test forward stops at punctuation after whitespace`() {
    doTest(",w", "${c}foo ;bar", "foo ${c};bar", Mode.NORMAL())
  }

  @Test
  fun `test forward treats a run of brackets as one word`() {
    // Upstream stops on each bracket; regular Vim w treats "(((" as a single punctuation word.
    doTest(",w", "a${c}(((b", "a(((${c}b", Mode.NORMAL())
  }

  @Test
  fun `test backward stops at word start after punctuation`() {
    doTest(",b", "foo.ba${c}r", "foo.${c}bar", Mode.NORMAL())
  }

  @Test
  fun `test forward skips a hyphen delimiter`() {
    doTest(",w", "${c}foo-bar", "foo-${c}bar", Mode.NORMAL())
  }

  @Test
  fun `test end stops before a hyphen delimiter`() {
    doTest(",e", "${c}foo-bar", "fo${c}o-bar", Mode.NORMAL())
  }
}

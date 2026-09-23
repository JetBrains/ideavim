/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchit

import com.intellij.ide.highlighter.HtmlFileType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for user-defined matchit pairs declared via the `b:match_words` buffer variable.
 *
 * See `:help b:match_words` (vim-matchit/doc/matchit.txt:252). The value is a comma-separated list of groups,
 * each group being a colon-separated list of patterns: `ini:mid:...:fin`. `%` cycles forward through a group.
 *
 * These tests deliberately use a plain text file, so that no built-in [FileTypePatterns] entry can satisfy them -
 * the only source of the pair is the user's own `b:match_words`.
 */
class MatchitMatchWordsTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchit")
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test jump from user-defined opening word to closing word`() {
    doTest(
      "%",
      """
        procedure CalculateTotals;
        ${c}begin
          Total := Total + Price;
        end
      """.trimIndent(),
      """
        procedure CalculateTotals;
        begin
          Total := Total + Price;
        ${c}end
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("let b:match_words = 'begin:end'") },
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test jump from user-defined closing word back to opening word`() {
    doTest(
      "%",
      """
        procedure CalculateTotals;
        begin
          Total := Total + Price;
        ${c}end
      """.trimIndent(),
      """
        procedure CalculateTotals;
        ${c}begin
          Total := Total + Price;
        end
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("let b:match_words = 'begin:end'") },
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test cycle through a three pattern group`() {
    doTest(
      "%",
      """
        ${c}if Total > Limit then
          Warn;
        else
          Accept;
        endif
      """.trimIndent(),
      """
        if Total > Limit then
          Warn;
        ${c}else
          Accept;
        endif
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("let b:match_words = 'if:else:endif'") },
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test nested pairs are skipped`() {
    doTest(
      "%",
      """
        ${c}begin
          begin
            Total := Total + Price;
          end
        end
      """.trimIndent(),
      """
        begin
          begin
            Total := Total + Price;
          end
        ${c}end
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("let b:match_words = 'begin:end'") },
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test reverse motion cycles backwards through a three pattern group`() {
    doTest(
      "g%",
      """
        ${c}if Total > Limit then
          Warn;
        else
          Accept;
        endif
      """.trimIndent(),
      """
        if Total > Limit then
          Warn;
        else
          Accept;
        ${c}endif
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("let b:match_words = 'if:else:endif'") },
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test second group of a comma separated list`() {
    doTest(
      "%",
      """
        ${c}repeat
          Total := Total + Price;
        until Total > Limit
      """.trimIndent(),
      """
        repeat
          Total := Total + Price;
        ${c}until Total > Limit
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("let b:match_words = 'begin:end,repeat:until'") },
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test user patterns are merged with the built in patterns of the file type`() {
    doTest(
      "%",
      """
        <div>
          ${c}begin
          end
        </div>
      """.trimIndent(),
      """
        <div>
          begin
          ${c}end
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
      afterEditorInitialized = { enterCommand("let b:match_words = 'begin:end'") },
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test vim word boundary atoms are honoured`() {
    doTest(
      "%",
      """
        ${c}if Total > Limit then
          Warn;
        endif
      """.trimIndent(),
      """
        if Total > Limit then
          Warn;
        ${c}endif
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("""let b:match_words = '\<if\>:\<endif\>'""") },
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test group with more than one middle pattern`() {
    doTest(
      "%",
      """
        ${c}if Total > Limit then
          Warn;
        elsif Total > 0 then
          Accept;
        else
          Reject;
        end
      """.trimIndent(),
      """
        if Total > Limit then
          Warn;
        ${c}elsif Total > 0 then
          Accept;
        else
          Reject;
        end
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("let b:match_words = 'if:elsif:else:end'") },
    )
  }

  /**
   * `:` and `,` are the separators of `b:match_words`, so a pattern that needs them literally has to escape them:
   * `\:` and `\,` (see `:help b:match_words`, vim-matchit/doc/matchit.txt:254). The escaped separator must not
   * split the group, and the backslash must be stripped before the pattern reaches the regex engine.
   *
   * PHP's alternative syntax is the canonical case - the opening pattern genuinely ends with a colon.
   */
  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test escaped colon does not split the group`() {
    doTest(
      "%",
      """
        ${c}foreach (${'$'}items as ${'$'}item):
          echo ${'$'}item;
        endforeach;
      """.trimIndent(),
      """
        foreach (${'$'}items as ${'$'}item):
          echo ${'$'}item;
        ${c}endforeach;
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("""let b:match_words = 'foreach\s*(.*)\s*\::endforeach'""") },
    )
  }

  /**
   * Same rule as for `\:`, but for the group separator: a pattern that needs a literal comma has to write it
   * as `\,`, otherwise it would start a new group (see `:help b:match_words`).
   */
  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test escaped comma does not split the list of groups`() {
    doTest(
      "%",
      """
        ${c}section alpha, beta
          threshold = 10
        endsection
      """.trimIndent(),
      """
        section alpha, beta
          threshold = 10
        ${c}endsection
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("""let b:match_words = 'section .*\, .*:endsection'""") },
    )
  }

  /**
   * Matchit appends `'matchpairs'` *after* `b:match_words` when it builds its search pattern
   * (vim-matchit/autoload/matchit.vim:81-84), and when several patterns match the same spot it picks the one listed
   * first (vim-matchit/doc/matchit.txt:103). So a user pattern that starts on the cursor beats a plain bracket
   * sitting on that very character - which is what makes template tags such as `{% ... %}` usable at all.
   *
   * IdeaVim short-circuits on `DEFAULT_PAIRS` before it ever looks at the patterns, so the bracket always wins.
   */
  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test user pattern wins over a bracket under the cursor`() {
    doTest(
      "%",
      """
        ${c}{% for item in items %}
          <p>{{ item }}</p>
        {% endfor %}
      """.trimIndent(),
      """
        {% for item in items %}
          <p>{{ item }}</p>
        ${c}{% endfor %}
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("""let b:match_words = '{% for .* %}:{% endfor %}'""") },
    )
  }

  /**
   * In operator-pending mode the motion is inclusive and has to cover the whole closing pattern, not just its first
   * character - that is what `isInOpPending` is for. The built-in patterns are covered by the other suites, but the
   * user-defined ones never went through this path.
   */
  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test operator pending motion covers the whole closing pattern`() {
    doTest(
      "d%",
      """
        ${c}begin
          Total := Total + Price;
        end
      """.trimIndent(),
      "",
      afterEditorInitialized = { enterCommand("let b:match_words = 'begin:end'") },
    )
  }

  /**
   * A typo in `~/.ideavimrc` must not blow up in the middle of a motion. Vim reports a bad pattern and leaves the
   * cursor alone; the worst we may do is fall back to the default `%` behaviour.
   */
  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test malformed user pattern does not break the motion`() {
    doTest(
      "%",
      """
        ${c}begin
          Total := Total + Price;
        end
      """.trimIndent(),
      """
        ${c}begin
          Total := Total + Price;
        end
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("""let b:match_words = 'begin\%(:end'""") },
    )
  }

  /**
   * `%` is a per-caret motion, so every caret has to resolve its own pair against the same `b:match_words`.
   */
  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test every caret jumps to its own pair`() {
    doTest(
      "%",
      """
        ${c}begin
          Total := Total + Price;
        end

        ${c}begin
          Total := Total - Discount;
        end
      """.trimIndent(),
      """
        begin
          Total := Total + Price;
        ${c}end

        begin
          Total := Total - Discount;
        ${c}end
      """.trimIndent(),
      afterEditorInitialized = { enterCommand("let b:match_words = 'begin:end'") },
    )
  }

  /**
   * The worked example from `:help b:match_words` (vim-matchit/doc/matchit.txt:109-115): with
   * `<:>,<tag>:</tag>`, the `<` group is listed first, so it wins when the cursor sits on the angle bracket.
   */
  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test earlier group wins when both match at the cursor`() {
    doTest(
      "%",
      """a ${c}<tag> is born</tag>""",
      """a <tag${c}> is born</tag>""",
      afterEditorInitialized = { enterCommand("let b:match_words = '<:>,<tag>:</tag>'") },
    )
  }

  /**
   * Same setting, but with the cursor one character to the right: now only `<tag>` contains the cursor, so it is
   * preferred over `<` and `%` jumps to the closing tag (vim-matchit/doc/matchit.txt:113-115).
   */
  @Test
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test group containing the cursor wins over an earlier one`() {
    doTest(
      "%",
      """a <${c}tag> is born</tag>""",
      """a <tag> is born${c}</tag>""",
      afterEditorInitialized = { enterCommand("let b:match_words = '<:>,<tag>:</tag>'") },
    )
  }
}

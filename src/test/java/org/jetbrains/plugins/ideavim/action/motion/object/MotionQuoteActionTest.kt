/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for quote text objects (i", a", i', a', i`, a`) which use preserveSelectionAnchor = false.
 *
 * preserveSelectionAnchor = false affects visual mode behavior:
 * - Selection anchor is reset to block start when applying text object
 * - Entire block is selected regardless of selection direction
 */
@Suppress("SpellCheckingInspection")
class MotionQuoteActionTest : VimTestCase() {

  // ============== Inner Double Quote (i") ==============

  @Test
  fun `test inner double quote from middle of quoted text`() {
    doTest(
      "vi\"",
      "foo \"bar b${c}az qux\" quux",
      "foo \"${s}bar baz qu${c}x${se}\" quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner double quote from start of quoted text`() {
    doTest(
      "vi\"",
      "foo \"${c}bar baz qux\" quux",
      "foo \"${s}bar baz qu${c}x${se}\" quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner double quote from before quote`() {
    doTest(
      "vi\"",
      "foo ${c}\"bar baz qux\" quux",
      "foo \"${s}bar baz qu${c}x${se}\" quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner double quote with backwards selection`() {
    doTest(
      listOf("v", "h", "i\""),
      "foo \"bar b${c}az qux\" quux",
      "foo \"${s}${c}bar baz qux${se}\" quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(shouldBeFixed = true)
  fun `test inner double quote empty string`() {
    // IdeaVim selects the closing quote even for empty string
    doTest(
      "vi\"",
      "foo \"${c}\" bar",
      "foo \"${s}${c}\"${se} bar",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Outer Double Quote (a") ==============

  @Test
  fun `test outer double quote from middle of quoted text`() {
    doTest(
      "va\"",
      "foo \"bar b${c}az qux\" quux",
      """foo $s"bar baz qux"$c ${se}quux""",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer double quote with backwards selection`() {
    doTest(
      listOf("v", "h", "a\""),
      "foo \"bar b${c}az qux\" quux",
      "foo ${s}${c}\"bar baz qux\" ${se}quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Outer Quote Whitespace Handling ==============
  // From Vim docs: "Any trailing white space is included, unless there is none, then leading white space is included"

  @Test
  fun `test outer quote with trailing whitespace includes trailing whitespace`() {
    // Trailing whitespace exists, so it should be included (not leading)
    doTest(
      "va\"",
      "before      \"hel${c}lo\"      after",
      "before      ${s}\"hello\"     ${c} ${se}after",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer quote without trailing whitespace includes leading whitespace`() {
    // No trailing whitespace (quote at end), so leading whitespace should be included
    doTest(
      "va\"",
      "before      \"hel${c}lo\"",
      "before${s}      \"hello${c}\"${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer quote at end of word includes leading whitespace`() {
    // Quote immediately followed by non-whitespace, so leading whitespace should be included
    doTest(
      "va\"",
      "before      \"hel${c}lo\".after",
      "before${s}      \"hello${c}\"${se}.after",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Inner Single Quote (i') ==============

  @Test
  fun `test inner single quote from middle of quoted text`() {
    doTest(
      "vi'",
      "foo 'bar b${c}az qux' quux",
      "foo '${s}bar baz qu${c}x${se}' quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner single quote with backwards selection`() {
    doTest(
      listOf("v", "h", "i'"),
      "foo 'bar b${c}az qux' quux",
      "foo '${s}${c}bar baz qux${se}' quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Outer Single Quote (a') ==============

  @Test
  fun `test outer single quote from middle of quoted text`() {
    doTest(
      "va'",
      "foo 'bar b${c}az qux' quux",
      "foo ${s}'bar baz qux'${c} ${se}quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer single quote with backwards selection`() {
    doTest(
      listOf("v", "h", "a'"),
      "foo 'bar b${c}az qux' quux",
      "foo ${s}${c}'bar baz qux' ${se}quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Inner Backtick (i`) ==============

  @Test
  fun `test inner backtick from middle of quoted text`() {
    doTest(
      "vi`",
      "foo `bar b${c}az qux` quux",
      "foo `${s}bar baz qu${c}x${se}` quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner backtick with backwards selection`() {
    doTest(
      listOf("v", "h", "i`"),
      "foo `bar b${c}az qux` quux",
      "foo `${s}${c}bar baz qux${se}` quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Outer Backtick (a`) ==============

  @Test
  fun `test outer backtick from middle of quoted text`() {
    doTest(
      "va`",
      "foo `bar b${c}az qux` quux",
      "foo ${s}`bar baz qux`${c} ${se}quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer backtick with backwards selection`() {
    doTest(
      listOf("v", "h", "a`"),
      "foo `bar b${c}az qux` quux",
      "foo ${s}${c}`bar baz qux` ${se}quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Delete with Quote Text Object ==============

  @Test
  fun `test delete inner double quote`() {
    doTest(
      "di\"",
      "foo \"bar b${c}az qux\" quux",
      "foo \"${c}\" quux",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test delete outer double quote`() {
    // IdeaVim doesn't delete trailing space (unlike some Vim behaviors)
    doTest(
      "da\"",
      "foo \"bar b${c}az qux\" quux",
      "foo ${c}quux",
      Mode.NORMAL(),
    )
  }

  // ============== Change with Quote Text Object ==============

  @Test
  fun `test change inner double quote`() {
    doTest(
      "ci\"",
      "foo \"bar b${c}az qux\" quux",
      "foo \"${c}\" quux",
      Mode.INSERT,
    )
  }

  // ============== Yank with Quote Text Object ==============

  @Test
  fun `test yank inner double quote`() {
    doTest(
      "yi\"",
      "foo \"bar b${c}az qux\" quux",
      "foo \"${c}bar baz qux\" quux",
      Mode.NORMAL(),
    )
  }

  // ============== Nested Quotes ==============

  @Test
  fun `test inner double quote with single quotes inside`() {
    doTest(
      "vi\"",
      "foo \"bar 'b${c}az' qux\" quux",
      "foo \"${s}bar 'baz' qu${c}x${se}\" quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner single quote with double quotes inside`() {
    doTest(
      "vi'",
      "foo 'bar \"b${c}az\" qux' quux",
      "foo '${s}bar \"baz\" qu${c}x${se}' quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Edge Cases (from MotionInnerBlockDoubleQuoteActionTest) ==============

  @Test
  fun `test delete inner double quote from outside quotes`() {
    doTest("di\"", "${c}print(\"hello\")", "print(\"$c\")", Mode.NORMAL())
  }

  // ============== Quote selection scenarios ==============

  @Test
  fun `test vi quote inside quoted text selects inner content`() {
    doTest(
      "vi\"",
      "before \"hel${c}lo\" after",
      "before \"${s}hell${c}o${se}\" after",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test vi quote outside jumps to quoted text on the right`() {
    doTest(
      "vi\"",
      "bef${c}ore \"hello\" after",
      "before \"${s}hell${c}o${se}\" after",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    originalVimAfter = "before \"${s}hell${c}o${se}\" after",
    description = "IdeaVim doesn't find quote to the left when in backwards visual selection outside quotes",
    shouldBeFixed = true
  )
  fun `test vi quote with backwards selection jumps to quoted text on the left`() {
    // Start visual mode, move left, then vi" should find quote to the left
    // IdeaVim keeps the visual selection unchanged since it doesn't find a quote
    doTest(
      listOf("v", "h", "h", "i\""),
      "before \"hello\" af${c}ter",
      "before \"hello\" ${s}${c}aft${se}er",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    description = "On second vi\" selection, we should select other of the quotes",
    shouldBeFixed = true
  )
  fun `test repeated vi quote expands to outer quote in nested quotes`() {
    // First vi" selects inner quote content, second i" expands to outer quote content
    doTest(
      listOf("vi\"", "i\""),
      "outer \"first 'sec${c}ond' third\" end",
      "outer \"${s}first 'second' thir${c}d${se}\" end",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}

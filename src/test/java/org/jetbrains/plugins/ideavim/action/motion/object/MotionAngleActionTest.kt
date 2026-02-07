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
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for bracket text objects from MotionAngleAction.kt which use preserveSelectionAnchor = false.
 * This includes angle brackets (<>), braces ({}), square brackets ([]), and parentheses (()).
 *
 * preserveSelectionAnchor = false affects visual mode behavior:
 * - Selection anchor is reset to block start when applying text object
 * - Entire block is selected regardless of selection direction
 */
@Suppress("SpellCheckingInspection")
class MotionAngleActionTest : VimTestCase() {

  // ============== Inner Angle Bracket (i<) ==============

  @Test
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, description = "Because of some reason the neovim hangs when trying to get the text")
  fun `test inner angle bracket from middle of content`() {
    doTest(
      "vi<",
      "foo <bar b${c}az qux> quux",
      "foo <${s}bar baz qu${c}x${se}> quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, description = "Because of some reason the neovim hangs when trying to get the text")
  fun `test inner angle bracket with backwards selection`() {
    doTest(
      listOf("v", "h", "i<"),
      "foo <bar b${c}az qux> quux",
      "foo <${s}${c}bar baz qux${se}> quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Outer Angle Bracket (a<) ==============

  @Test
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, description = "Because of some reason the neovim hangs when trying to get the text")
  fun `test outer angle bracket from middle of content`() {
    doTest(
      "va<",
      "foo <bar b${c}az qux> quux",
      "foo ${s}<bar baz qux${c}>${se} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, description = "Because of some reason the neovim hangs when trying to get the text")
  fun `test outer angle bracket with backwards selection`() {
    doTest(
      listOf("v", "h", "a<"),
      "foo <bar b${c}az qux> quux",
      "foo ${s}${c}<bar baz qux>${se} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Inner Brace (i{) ==============

  @Test
  fun `test inner brace from middle of content`() {
    doTest(
      "vi{",
      "foo {bar b${c}az qux} quux",
      "foo {${s}bar baz qu${c}x${se}} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = """
      Vim for some operations keeps the direction and for some it doesn't.
      However, this looks like a bug in Vim.
      So, in IdeaVim we always keep the direction.
    """
  )
  fun `test inner brace with backwards selection`() {
    doTest(
      listOf("v", "h", "i{"),
      "foo {bar b${c}az qux} quux",
      "foo {${s}${c}bar baz qux${se}} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = """
      This is a change in behavior to match a more convinient work.
      In Vim the \n character is also selected
    """)
  fun `test inner brace multiline`() {
    // IdeaVim selects content starting after newline following opening brace
    doTest(
      "vi{",
      """
        |function() {
        |  let x${c} = 1;
        |  return x;
        |}
      """.trimMargin(),
      """
        |function() {
        |${s}  let x = 1;
        |  return x${c};${se}
        |}
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Outer Brace (a{) ==============

  @Test
  fun `test outer brace from middle of content`() {
    doTest(
      "va{",
      "foo {bar b${c}az qux} quux",
      "foo ${s}{bar baz qux${c}}${se} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = """
      Vim for some operations keeps the direction and for some it doesn't.
      However, this looks like a bug in Vim.
      So, in IdeaVim we always keep the direction.
    """
  )
  fun `test outer brace with backwards selection`() {
    doTest(
      listOf("v", "h", "a{"),
      "foo {bar b${c}az qux} quux",
      "foo ${s}${c}{bar baz qux}${se} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Inner Square Bracket (i[) ==============

  @Test
  fun `test inner square bracket from middle of content`() {
    doTest(
      "vi[",
      "foo [bar b${c}az qux] quux",
      "foo [${s}bar baz qu${c}x${se}] quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = """
      Vim for some operations keeps the direction and for some it doesn't.
      However, this looks like a bug in Vim.
      So, in IdeaVim we always keep the direction.
    """
  )
  fun `test inner square bracket with backwards selection`() {
    doTest(
      listOf("v", "h", "i["),
      "foo [bar b${c}az qux] quux",
      "foo [${s}${c}bar baz qux${se}] quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Outer Square Bracket (a[) ==============

  @Test
  fun `test outer square bracket from middle of content`() {
    doTest(
      "va[",
      "foo [bar b${c}az qux] quux",
      "foo ${s}[bar baz qux${c}]${se} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = """
      Vim for some operations keeps the direction and for some it doesn't.
      However, this looks like a bug in Vim.
      So, in IdeaVim we always keep the direction.
    """
  )
  fun `test outer square bracket with backwards selection`() {
    doTest(
      listOf("v", "h", "a["),
      "foo [bar b${c}az qux] quux",
      "foo ${s}${c}[bar baz qux]${se} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Delete operations ==============

  @Test
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, description = "Because of some reason the neovim hangs when trying to get the text")
  fun `test delete inner angle bracket`() {
    doTest(
      "di<",
      "foo <bar b${c}az qux> quux",
      "foo <${c}> quux",
      Mode.NORMAL(),
    )
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, description = "Because of some reason the neovim hangs when trying to get the text")
  fun `test delete outer angle bracket`() {
    doTest(
      "da<",
      "foo <bar b${c}az qux> quux",
      "foo ${c} quux",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test delete inner brace`() {
    doTest(
      "di{",
      "foo {bar b${c}az qux} quux",
      "foo {${c}} quux",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test delete outer brace`() {
    doTest(
      "da{",
      "foo {bar b${c}az qux} quux",
      "foo ${c} quux",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test delete inner square bracket`() {
    doTest(
      "di[",
      "foo [bar b${c}az qux] quux",
      "foo [${c}] quux",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test delete outer square bracket`() {
    doTest(
      "da[",
      "foo [bar b${c}az qux] quux",
      "foo ${c} quux",
      Mode.NORMAL(),
    )
  }

  // ============== Nested brackets ==============

  @Test
  fun `test inner brace with nested braces`() {
    doTest(
      "vi{",
      "foo {bar {b${c}az} qux} quux",
      "foo {bar {${s}ba${c}z${se}} qux} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer brace with nested braces`() {
    doTest(
      "va{",
      "foo {bar {b${c}az} qux} quux",
      "foo {bar ${s}{baz${c}}${se} qux} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated inner brace expands to outer`() {
    doTest(
      listOf("vi{", "i{"),
      "foo {bar {b${c}az} qux} quux",
      "foo {${s}bar {baz} qu${c}x${se}} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}

/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionOuterBlockBraceActionTest : VimTestCase() {
  @Test
  fun testOutside() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "${c}foo{bar}\n",
    )
    assertState("foo{}\n")
  }

  @Test
  fun testOutsideInString() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "\"1${c}23\"foo{bar}\n",
    )
    assertState("\"123\"foo{}\n")
  }

  @Test
  fun testOutsideInString2() {
    typeTextInFile(
      injector.parser.parseKeys("di}"),
      "\"1${c}23{dsa}d\"foo{bar}\n",
    )
    assertState("\"123{}d\"foo{bar}\n")
  }

  // ============== preserveSelectionAnchor behavior tests ==============

  @Test
  fun `test inner brace from middle of content`() {
    doTest(
      "vi}",
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
      listOf("v", "h", "i}"),
      "foo {bar b${c}az qux} quux",
      "foo {${s}${c}bar baz qux${se}} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer brace from middle of content`() {
    doTest(
      "va}",
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
      listOf("v", "h", "a}"),
      "foo {bar b${c}az qux} quux",
      "foo ${s}${c}{bar baz qux}${se} quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}

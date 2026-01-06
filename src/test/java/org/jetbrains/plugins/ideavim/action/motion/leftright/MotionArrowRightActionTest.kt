/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionArrowRightActionTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test with inlay related to preceding text and block caret`() {
    val before = "I f${c}ound it in a legendary land"
    val after = "I fo${c}und it in a legendary land"
    configureByText(before)
    assertOffset(3)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 3/visual column 3.
    // Moving <Right> from offset 3 (visual column 3) to offset 4 should position the caret between the inlay and its
    // related text, at visual column 4, but then the block caret would paint over the inlay, which looks wrong.
    // Position at visual column 5 instead.
    // Before: "I f|o|«:test»und it in a legendary land"
    // After:  "I fo«:test»|u|nd it in a legendary land"
    addInlay(4, true, 5)

    typeText(injector.parser.parseKeys("<Right>"))
    assertState(after)

    assertOffset(4)
    assertVisualPosition(0, 5)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test with inlay related to preceding text and block caret 2`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I fou${c}nd it in a legendary land"
    configureByText(before)
    assertOffset(4)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 3/visual column 3.
    // Moving <Right> from offset 4 (visual column 5 for text) will move to offset 5, which is visual column 6.
    // Before: "I fo«:test»|u|nd it in a legendary land."
    // After: "I fo«:test»u|n|d it in a legendary land."
    addInlay(4, true, 5)

    typeText(injector.parser.parseKeys("<Right>"))
    assertState(after)

    assertOffset(5)
    assertVisualPosition(0, 6)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test with inlay related to preceding text and bar caret`() {
    val before = "I f${c}ound it in a legendary land"
    val after = "I fo${c}und it in a legendary land"
    configureByText(before)
    assertOffset(3)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 3/visual column 3.
    // Moving <Right> from offset 3 (visual column 3) to offset 4 should position the caret between the inlay and the
    // related text at visual column 4, which is the inlay. This is appropriate for the bar caret, which renders
    // "in between columns".
    // Before: "I f|o«:test»und it in a legendary land"
    // After:  "I fo|«:test»und it in a legendary land"
    addInlay(4, true, 5)

    typeText(injector.parser.parseKeys("i" + "<Right>"))
    assertState(after)

    assertOffset(4)
    assertVisualPosition(0, 4)

    typeText(injector.parser.parseKeys("<Esc>"))
    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test with inlay related to preceding text and bar caret 2`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I fou${c}nd it in a legendary land"
    configureByText(before)
    assertOffset(4)

    assertVisualPosition(0, 4)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 3/visual column 3.
    // Moving <Right> from offset 4 (visual column 4 because bar caret and related to preceding text!) will move to
    // offset 3, which is also visual column 3.
    // Initially (normal):  "I fo|u|nd it in a legendary land" (caret = vp4)
    // With inlay (normal): "I fo«:test»|u|nd it in a legendary land" (caret = vp5)
    // In insert mode:      "I fo|«:test»und it in a legendary land" (caret = vp4)
    // <Right>:             "I fo«:test»u|nd it in a legendary land" (caret = vp6)
    // <Esc>:               "I fo«:test»|u|nd it in a legendary land" (caret = vp5)
    addInlay(4, true, 5)

    typeText(injector.parser.parseKeys("i"))
    assertVisualPosition(0, 4)

    typeText(injector.parser.parseKeys("<Right>"))
    fixture.checkResult(after)

    assertOffset(5)
    assertVisualPosition(0, 6)

    typeText(injector.parser.parseKeys("<Esc>"))
    assertOffset(4)
    assertVisualPosition(0, 5)
  }

  // Kotlin parameter hints are a good example of inlays related to following text
  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test with inlay related to following text and block caret`() {
    val before = "I f${c}ound it in a legendary land"
    val after = "I fo${c}und it in a legendary land"
    configureByText(before)
    assertOffset(3)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 4/visual column 5.
    // Moving <Right> from offset 3 to offset 4 should position the caret between the inlay and the related text, at
    // visual column 5, which is fine for block caret
    // Before: "I f|o|«test:»und it in a legendary land."
    // After: "I fo«test:»|u|nd it in a legendary land."
    addInlay(4, false, 5)

    typeText(injector.parser.parseKeys("<Right>"))
    assertState(after)

    assertOffset(4)
    assertVisualPosition(0, 5)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @Test
  fun `test with inlay related to following text and bar caret`() {
    val before = "I f${c}ound it in a legendary land"
    val after = "I fo${c}und it in a legendary land"
    configureByText(before)
    assertOffset(3)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 4/visual column 5.
    // Moving <Right> from offset 3 to offset 4 should position the caret between the inlay and the related text, at
    // visual column 5, which is fine for bar caret
    // Before: "I f|o«test:»und it in a legendary land."
    // After: "I fo«test:»|und it in a legendary land."
    addInlay(4, false, 5)

    typeText(injector.parser.parseKeys("i" + "<Right>"))
    assertState(after)

    assertOffset(4)
    assertVisualPosition(0, 5)

    typeText(injector.parser.parseKeys("<Esc>"))
    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual default options`() {
    doTest(
      listOf("v", "<Right>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I ${s}f${c}o${se}und it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual stopsel`() {
    doTest(
      listOf("v", "<Right>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I f${c}ound it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set keymodel=stopsel")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual stopselect`() {
    doTest(
      listOf("v", "<Right>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I ${s}f${c}o${se}und it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    ) {
      enterCommand("set keymodel=stopselect")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual stopvisual`() {
    doTest(
      listOf("v", "<Right>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I f${c}ound it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set keymodel=stopvisual")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test visual stopvisual multicaret`() {
    doTest(
      listOf("v", "<Right>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I f${c}ound it in a legendary land
                all rocks and l${c}avender and tufted grass,
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set keymodel=stopvisual")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap in the same line`() {
    doTest(
      listOf("<Right>"),
      """
          Oh, hi M${c}ark
      """.trimIndent(),
      """
          Oh, hi Ma${c}rk
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=>")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap at file end`() {
    doTest(
      listOf("<Right>"),
      """
          Oh, hi Mar${c}k
      """.trimIndent(),
      """
          Oh, hi Mar${c}k
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=>")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap to next line`() {
    doTest(
      listOf("<Right>"),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark
          ${c}You are my favourite customer
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=>")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test from empty line to empty line`() {
    doTest(
      listOf("<Right>"),
      """
          Oh, hi Mark
          ${c}

          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark

          ${c}
          You are my favourite customer
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=>")
    }
  }
}

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
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption

@TraceOptions(TestOptionConstants.keymodel)
class MotionArrowLeftActionTest : VimTestCase() {

  // Kotlin type hints should be an obvious example of an inlay related to preceding text, but they are actually
  // related to following (KTIJ-3768). The inline rename options inlay is a better example
  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test with inlay related to preceding text and block caret`() {
    val before = "I fou${c}nd it in a legendary land"
    val after = "I fo${c}und it in a legendary land"
    configureByText(before)
    assertOffset(5)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 3/visual column 3.
    // Moving <Left> from offset 5 (visual column 6) to offset 4 should position the caret between the inlay and its
    // related text, at visual column 4, but then the block caret would paint over the inlay, which looks wrong.
    // Position at visual column 5 instead.
    // Before: "I fo«:test»u|n|d it in a legendary land"
    // After:  "I fo«:test»|u|nd it in a legendary land"
    addInlay(4, true, 5)

    typeText(injector.parser.parseKeys("<Left>"))
    assertState(after)

    assertOffset(4)
    assertVisualPosition(0, 5)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test with inlay related to preceding text and block caret 2`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    configureByText(before)
    assertOffset(4)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 3/visual column 3.
    // Moving <Left> from offset 4 (visual column 5 for text) will move to offset 3, which is also visual column 3.
    // Before: "I fo«:test»|u|nd it in a legendary land."
    // After: "I f|o|«:test»und it in a legendary land."
    addInlay(4, true, 5)

    typeText(injector.parser.parseKeys("<Left>"))
    assertState(after)

    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test with inlay related to preceding text and bar caret`() {
    val before = "I fou${c}nd it in a legendary land"
    val after = "I fo${c}und it in a legendary land"
    configureByText(before)
    assertOffset(5)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 3/visual column 3.
    // Moving <Left> from offset 5 (visual column 6) to offset 4 should position the caret between the inlay and the
    // related text at visual column 4, which is the inlay. This is appropriate for the bar caret, which renders
    // "in between columns".
    // Before: "I fo«:test»u|nd it in a legendary land"
    // After:  "I fo|«:test»und it in a legendary land"
    addInlay(4, true, 5)

    typeText(injector.parser.parseKeys("i" + "<Left>"))
    assertState(after)

    assertOffset(4)
    assertVisualPosition(0, 4)

    typeText(injector.parser.parseKeys("<Esc>"))
    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test with inlay related to preceding text and bar caret 2`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    configureByText(before)
    assertOffset(4)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 3/visual column 3.
    // Moving <Left> from offset 4 (visual column 5 for text) will move to offset 3, which is also visual column 3.
    // Before: "I fo«:test»|und it in a legendary land." (Actually, the caret will be "fo|«:test»und")
    // After: "I f|o«:test»und it in a legendary land."
    addInlay(4, true, 5)

    typeText(injector.parser.parseKeys("i" + "<Left>"))
    assertState(after)

    assertOffset(3)
    assertVisualPosition(0, 3)

    typeText(injector.parser.parseKeys("<Esc>"))
    assertOffset(2)
    assertVisualPosition(0, 2)
  }

  // Kotlin parameter hints are a good example of inlays related to following text
  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test with inlay related to following text with block caret`() {
    val before = "I fou${c}nd it in a legendary land"
    val after = "I fo${c}und it in a legendary land"
    configureByText(before)
    assertOffset(5)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 4/visual column 5.
    // Inlay shares offset 4 with the 'u' in "found" and inserts a new visual column 4.
    // Moving <Left> from offset 5 (visual column 6) to offset 4 should position the caret between the inlay and its
    // related text, at visual column 5, which is fine for the block caret.
    // Before: "I fo«test:»u|n|d it in a legendary land."
    // After: "I fo«test:»|u|nd it in a legendary land."
    addInlay(4, false, 5)

    typeText(injector.parser.parseKeys("<Left>"))
    assertState(after)

    assertOffset(4)
    assertVisualPosition(0, 5)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test with inlay related to following text with block caret 2`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    configureByText(before)
    assertOffset(4)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 4/visual column 5.
    // Moving <Left> from offset 4 (visual column 5 for text) will move to offset 3, which is also visual column 3.
    // Before: "I fo«test:»|u|nd it in a legendary land."
    // After: "I f|o|«test:»und it in a legendary land."
    addInlay(4, false, 5)

    typeText(injector.parser.parseKeys("<Left>"))
    assertState(after)

    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test with inlay related to following text with bar caret`() {
    val before = "I fou${c}nd it in a legendary land"
    val after = "I fo${c}und it in a legendary land"
    configureByText(before)
    assertOffset(5)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 4/visual column 5.
    // Moving <Left> from offset 5 (visual column 6) to offset 4 should position the caret between the inlay and its
    // related text, at visual column 5, which is fine for the bar caret.
    // Before: "I fo«test:»u|nd it in a legendary land."
    // After: "I fo«test:»|und it in a legendary land."
    addInlay(4, false, 5)

    typeText(injector.parser.parseKeys("i" + "<Left>"))
    assertState(after)

    assertOffset(4)
    assertVisualPosition(0, 5)

    typeText(injector.parser.parseKeys("<Esc>"))
    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.INLAYS)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test with inlay related to following text with bar caret 2`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    configureByText(before)
    assertOffset(4)

    // Inlay shares offset 4 with the 'u' in "found", inserts a new visual column 4 and is related to the text at
    // offset 4/visual column 5.
    // Moving <Left> from offset 4 (visual column 5 for text) will move to offset 3, which is also visual column 3.
    // Before: "I fo«test:»|und it in a legendary land."
    // After: "I f|o«test:»und it in a legendary land."
    addInlay(4, false, 5)

    typeText(injector.parser.parseKeys("i" + "<Left>"))
    assertState(after)

    assertOffset(3)
    assertVisualPosition(0, 3)

    typeText(injector.parser.parseKeys("<Esc>"))
    assertOffset(2)
    assertVisualPosition(0, 2)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test visual default options`() {
    doTest(
      listOf("v", "<Left>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I${s}${c} f${se}ound it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_stopsel]))
  fun `test visual stopsel`() {
    doTest(
      listOf("v", "<Left>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I${c} found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_stopselect]))
  fun `test visual stopselect`() {
    doTest(
      listOf("v", "<Left>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I${s}${c} f${se}ound it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_stopvisual]))
  fun `test visual stopvisual`() {
    doTest(
      listOf("v", "<Left>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I${c} found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_stopvisual]))
  fun `test visual stopvisual multicaret`() {
    doTest(
      listOf("v", "<Left>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I${c} found it in a legendary land
                all rocks and${c} lavender and tufted grass,
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test whichwrap in the same line`() {
    doTest(
      listOf("<Left>"),
      """
          Oh, hi Ma${c}rk
      """.trimIndent(),
      """
          Oh, hi M${c}ark
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=<")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test whichwrap at file start`() {
    doTest(
      listOf("<Left>"),
      """
          ${c}Oh, hi Mark
      """.trimIndent(),
      """
          ${c}Oh, hi Mark
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=<")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test whichwrap to previous line`() {
    doTest(
      listOf("<Left>"),
      """
          Oh, hi Mark
          ${c}You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=<")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test from empty line to empty line`() {
    doTest(
      listOf("<Left>"),
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
      enterCommand("set whichwrap=<")
    }
  }
}

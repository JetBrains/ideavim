/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.KeyModelOptionData
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

class MotionArrowLeftActionTest : VimOptionTestCase(KeyModelOptionData.name) {

  // Kotlin type hints should be an obvious example of an inlay related to preceding text, but they are actually
  // related to following (KTIJ-3768). The inline rename options inlay is a better example
  @VimOptionDefaultAll
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

    typeText(parseKeys("<Left>"))
    myFixture.checkResult(after)

    assertOffset(4)
    assertVisualPosition(0, 5)
  }

  @VimOptionDefaultAll
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

    typeText(parseKeys("<Left>"))
    myFixture.checkResult(after)

    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @VimOptionDefaultAll
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

    typeText(parseKeys("i", "<Left>"))
    myFixture.checkResult(after)

    assertOffset(4)
    assertVisualPosition(0, 4)

    typeText(parseKeys("<Esc>"))
    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @VimOptionDefaultAll
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

    typeText(parseKeys("i", "<Left>"))
    myFixture.checkResult(after)

    assertOffset(3)
    assertVisualPosition(0, 3)

    typeText(parseKeys("<Esc>"))
    assertOffset(2)
    assertVisualPosition(0, 2)
  }

  // Kotlin parameter hints are a good example of inlays related to following text
  @VimOptionDefaultAll
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

    typeText(parseKeys("<Left>"))
    myFixture.checkResult(after)

    assertOffset(4)
    assertVisualPosition(0, 5)
  }

  @VimOptionDefaultAll
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

    typeText(parseKeys("<Left>"))
    myFixture.checkResult(after)

    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @VimOptionDefaultAll
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

    typeText(parseKeys("i", "<Left>"))
    myFixture.checkResult(after)

    assertOffset(4)
    assertVisualPosition(0, 5)

    typeText(parseKeys("<Esc>"))
    assertOffset(3)
    assertVisualPosition(0, 3)
  }

  @VimOptionDefaultAll
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

    typeText(parseKeys("i", "<Left>"))
    myFixture.checkResult(after)

    assertOffset(3)
    assertVisualPosition(0, 3)

    typeText(parseKeys("<Esc>"))
    assertOffset(2)
    assertVisualPosition(0, 2)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionDefaultAll
  fun `test visual default options`() {
    doTest(
      listOf("v", "<Left>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I${s}${c} f${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      KeyModelOptionData.name,
      VimTestOptionType.LIST,
      [KeyModelOptionData.stopsel]
    )
  )
  fun `test visual stopsel`() {
    doTest(
      listOf("v", "<Left>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I${c} found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      KeyModelOptionData.name,
      VimTestOptionType.LIST,
      [KeyModelOptionData.stopselect]
    )
  )
  fun `test visual stopselect`() {
    doTest(
      listOf("v", "<Left>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I${s}${c} f${se}ound it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      KeyModelOptionData.name,
      VimTestOptionType.LIST,
      [KeyModelOptionData.stopvisual]
    )
  )
  fun `test visual stopvisual`() {
    doTest(
      listOf("v", "<Left>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I${c} found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      KeyModelOptionData.name,
      VimTestOptionType.LIST,
      [KeyModelOptionData.stopvisual]
    )
  )
  fun `test visual stopvisual multicaret`() {
    doTest(
      listOf("v", "<Left>"),
      """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I${c} found it in a legendary land
                all rocks and${c} lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }
}

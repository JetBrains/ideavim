/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.OptionValueType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefault
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption

class MotionArrowDownActionTest : VimOptionTestCase(OptionConstants.keymodelName, OptionConstants.virtualeditName) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionDefaultAll
  fun `test visual default options`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                wher${c}e${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.keymodelName,
      OptionValueType.STRING,
      OptionConstants.keymodel_stopsel
    )
  )
  @VimOptionDefault(OptionConstants.virtualeditName)
  fun `test visual stopsel`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.keymodelName,
      OptionValueType.STRING,
      OptionConstants.keymodel_stopselect
    )
  )
  @VimOptionDefault(OptionConstants.virtualeditName)
  fun `test visual stopselect`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                wher${c}e${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.keymodelName,
      OptionValueType.STRING,
      OptionConstants.keymodel_stopvisual
    )
  )
  @VimOptionDefault(OptionConstants.virtualeditName)
  fun `test visual stopvisual`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.keymodelName,
      OptionValueType.STRING,
      OptionConstants.keymodel_stopvisual
    )
  )
  @VimOptionDefault(OptionConstants.virtualeditName)
  fun `test visual stopvisual multicaret`() {
    doTest(
      listOf("v", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                wher${c}e it was settled on some sodden sand
                hard by the t${c}orrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""))
  @VimOptionDefault(OptionConstants.virtualeditName)
  fun `test char select stopsel`() {
    doTest(
      listOf("gh", "<Down>"),
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all ${s}rocks and lavender and tufted grass,
                where${c}${se} it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.SELECT,
      VimStateMachine.SubMode.VISUAL_CHARACTER
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test virtual edit down to shorter line`() {
    doTest(
      listOf("<Down>"),
      """
            class MyClass ${c}{
            }
      """.trimIndent(),
      """
            class MyClass {
            }${c}
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test virtual edit down to shorter line after dollar`() {
    doTest(
      listOf("$", "<Down>"),
      """
            class ${c}MyClass {
            }
      """.trimIndent(),
      """
            class MyClass {
            ${c}}
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  // Once you press '$', then any up or down actions stay on the end of the current line.
  // Any non up/down action breaks this.
  private val start = """
            what ${c}a long line I am
            yet I am short
            Lo and behold, I am the longest yet
            nope.
  """.trimIndent()

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test up and down after dollar`() {
    // Arrow keys
    doTest(
      listOf("$", "<Down>"), start,
      """
            what a long line I am
            yet I am shor${c}t
            Lo and behold, I am the longest yet
            nope.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test up and down after dollar1`() {
    doTest(
      listOf("$", "<Down>", "<Down>"), start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest ye${c}t
            nope.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test up and down after dollar2`() {
    doTest(
      listOf("$", "<Down>", "<Down>", "<Down>"), start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest yet
            nope${c}.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test up and down after dollar3`() {
    doTest(
      listOf("$", "<Down>", "<Down>", "<Down>", "<Up>"), start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest ye${c}t
            nope.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test up and down after dollar4`() {
    // j k keys

    doTest(
      listOf("$", "j"), start,
      """
            what a long line I am
            yet I am shor${c}t
            Lo and behold, I am the longest yet
            nope.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test up and down after dollar5`() {
    doTest(
      listOf("$", "j", "j"), start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest ye${c}t
            nope.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test up and down after dollar6`() {
    doTest(
      listOf("$", "j", "j", "j"), start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest yet
            nope${c}.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore)
  )
  fun `test up and down after dollar7`() {
    doTest(
      listOf("$", "j", "j", "j", "k"), start,
      """
            what a long line I am
            yet I am short
            Lo and behold, I am the longest ye${c}t
            nope.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.keymodelName,
      OptionValueType.STRING,
      OptionConstants.keymodel_stopselect
    )
  )
  @VimOptionDefault(OptionConstants.virtualeditName)
  fun `test char select simple move`() {
    doTest(
      listOf("gH", "<Down>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.keymodelName,
      OptionValueType.STRING,
      OptionConstants.keymodel_stopselect
    )
  )
  @VimOptionDefault(OptionConstants.virtualeditName)
  fun `test select multiple carets`() {
    doTest(
      listOf("gH", "<Down>"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionDefaultAll
  fun `test arrow down in insert mode scrolls caret at scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(injector.parser.parseKeys("i" + "<Down>"))
    assertPosition(30, 0)
    assertVisibleArea(10, 44)
  }
}

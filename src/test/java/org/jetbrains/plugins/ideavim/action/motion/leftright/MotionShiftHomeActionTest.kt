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
import org.jetbrains.plugins.ideavim.OptionValueType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.junit.jupiter.api.Test

class MotionShiftHomeActionTest : VimOptionTestCase(OptionConstants.keymodel, OptionConstants.selectmode) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionDefaultAll
  @Test
  fun `test simple home`() {
    val keys = listOf("<S-Home>")
    val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimOptionDefaultAll
  @Test
  fun `test default continueselect`() {
    val keymodel = optionsNoEditor().getStringListValues(OptionConstants.keymodel)
    kotlin.test.assertTrue(OptionConstants.keymodel_continueselect in keymodel)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodel, OptionValueType.STRING, OptionConstants.keymodel_startsel),
    VimTestOption(OptionConstants.selectmode, OptionValueType.STRING, ""),
  )
  @Test
  fun `test start visual`() {
    val keys = listOf("<S-Home>")
    val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${s}${c}I found it in a l${se}egendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodel, OptionValueType.STRING, OptionConstants.keymodel_startsel),
    VimTestOption(OptionConstants.selectmode, OptionValueType.STRING, OptionConstants.selectmode_key),
  )
  @Test
  fun `test start select`() {
    val keys = listOf("<S-Home>")
    val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${s}${c}I found it in a ${se}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodel, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.selectmode, OptionValueType.STRING, ""),
  )
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test continue visual`() {
    val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${s}${c}I found it in a legendary land${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("<S-Home>"))
    assertState(VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    typeText(injector.parser.parseKeys("\$v" + "<S-Home>"))
    assertState(after)
    assertState(VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodel, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.selectmode, OptionValueType.STRING, ""),
  )
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test continue select`() {
    val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${s}${c}I found it in a legendary lan${se}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("<S-Home>"))
    assertState(VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    typeText(injector.parser.parseKeys("\$gh" + "<S-Home>"))
    assertState(after)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }
}

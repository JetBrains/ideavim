/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.api.Options
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

@TraceOptions(TestOptionConstants.keymodel, TestOptionConstants.selectmode)
class MotionShiftHomeActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, doesntAffectTest = true),
    VimOption(TestOptionConstants.selectmode, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.keymodel, doesntAffectTest = true),
    VimOption(TestOptionConstants.selectmode, doesntAffectTest = true),
  )
  fun `test default continueselect`() {
    val keymodel = optionsNoEditor().getStringListValues(Options.keymodel)
    kotlin.test.assertTrue(OptionConstants.keymodel_continueselect in keymodel)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
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
  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_startsel]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [OptionConstants.selectmode_key]),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [""]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
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

  @OptionTest(
    VimOption(TestOptionConstants.keymodel, limitedValues = [""]),
    VimOption(TestOptionConstants.selectmode, limitedValues = [""]),
  )
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
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

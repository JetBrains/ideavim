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

class MotionShiftEndActionTest : VimOptionTestCase(OptionConstants.keymodel, OptionConstants.selectmode) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionDefaultAll
  fun `test simple end`() {
    val keys = listOf("<S-End>")
    val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodel, OptionValueType.STRING, OptionConstants.keymodel_startsel),
    VimTestOption(OptionConstants.selectmode, OptionValueType.STRING, ""),
  )
  fun `test start visual`() {
    val keys = listOf("<S-End>")
    val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a ${s}legendary land${c}${se}
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
  fun `test start select`() {
    val keys = listOf("<S-End>")
    val before = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a ${s}legendary land${c}${se}
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

            ${s}I found it in a legendary land${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("<S-End>"))
    assertState(VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    typeText(injector.parser.parseKeys("0v" + "<S-End>"))
    assertState(after)
    assertState(VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  @VimOptionTestConfiguration(
    VimTestOption(OptionConstants.keymodel, OptionValueType.STRING, ""),
    VimTestOption(OptionConstants.selectmode, OptionValueType.STRING, ""),
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

            ${s}I found it in a legendary land${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("<S-End>"))
    assertState(VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    typeText(injector.parser.parseKeys("0gh" + "<S-End>"))
    assertState(after)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }
}

/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.OptionValueType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption

class MotionHomeActionTest : VimOptionTestCase(OptionConstants.keymodelName) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionDefaultAll
  fun `test motion home`() {
    val keys = "<Home>"
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
  fun `test default stop select`() {
    val keymodel = (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.keymodelName) as VimString).value
    assertTrue(OptionConstants.keymodel_stopselect in keymodel)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""))
  fun `test continue visual`() {
    val keys = listOf("v", "<Home>")
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
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.keymodelName, OptionValueType.STRING, ""))
  fun `test continue select`() {
    val keys = listOf("gh", "<Home>")
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

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.keymodelName,
      OptionValueType.STRING,
      OptionConstants.keymodel_stopvisual
    )
  )
  fun `test exit visual`() {
    val keys = listOf("v", "<Home>")
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

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(
      OptionConstants.keymodelName,
      OptionValueType.STRING,
      OptionConstants.keymodel_stopselect
    )
  )
  fun `test exit select`() {
    val keys = listOf("gh", "<Home>")
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
}

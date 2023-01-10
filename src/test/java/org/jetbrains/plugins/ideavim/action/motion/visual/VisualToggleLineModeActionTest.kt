/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.visual

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.VimTestCase

class VisualToggleLineModeActionTest : VimTestCase() {
  fun `test enter visual with count`() {
    doTest(
      "1V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    ${s}I ${c}found it in a legendary land
                    ${se}all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_LINE
    )
  }

  fun `test enter visual with count multicaret`() {
    doTest(
      "1V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it ${c}was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    ${s}I ${c}found it in a legendary land
                    ${se}all rocks and lavender and tufted grass,
                    ${s}where it ${c}was settled on some sodden sand
                    ${se}hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_LINE
    )
  }

  fun `test enter visual with 3 count`() {
    doTest(
      "3V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    ${s}I found it in a legendary land
                    all rocks and lavender and tufted grass,
                    wh${c}ere it was settled on some sodden sand
                    ${se}hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_LINE
    )
  }

  fun `test enter visual with 100 count`() {
    doTest(
      "100V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    ${s}I found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    ha${c}rd by the torrent of a mountain pass.${se}
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_LINE
    )
  }

  fun `test selectmode option`() {
    configureByText(
      """
                    A Discovery

                    I${c} found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand[long line]
                    hard by the torrent of a mountain pass.
      """.trimIndent()
    )
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.selectmodeName, VimString("cmd"))
    typeText(injector.parser.parseKeys("V"))
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_LINE)
  }
}

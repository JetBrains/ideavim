/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption

@TraceOptions(TestOptionConstants.keymodel)
class SelectMotionLeftActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(
    VimOption(
      TestOptionConstants.keymodel,
      limitedValues = [OptionConstants.keymodel_stopselect],
    ),
  )
  fun `test char select simple move`() {
    doTest(
      listOf("viw", "<C-G>", "<Left>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I foun${c}d it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_stopselect]))
  fun `test select multiple carets`() {
    doTest(
      listOf("viwo", "<C-G>", "<Left>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                ${c}Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I${c} found it in a legendary land
                consectetur adipiscing elit
                ${c}Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [""]))
  fun `test without stopsel`() {
    doTest(
      listOf("viw", "<C-G>", "<Left>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I ${s}foun${c}${se}d it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }
}

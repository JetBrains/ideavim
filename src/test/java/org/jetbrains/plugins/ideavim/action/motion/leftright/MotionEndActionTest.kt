/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

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
class MotionEndActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test motion end`() {
    val keys = listOf("<End>")
    val before = """
            Lorem Ipsum

            I found it in a ${c}legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I found it in a legendary lan${c}d
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [""]))
  fun `test continue visual`() {
    val keys = listOf("v", "<End>")
    val before = """
            Lorem Ipsum

            I found it in a ${c}legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I found it in a ${s}legendary land${c}${se}
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [""]))
  fun `test continue select`() {
    val keys = listOf("gh", "<End>")
    val before = """
            Lorem Ipsum

            I found it in a ${c}legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I found it in a ${s}legendary land${c}${se}
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.SELECT(SelectionType.CHARACTER_WISE))
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_stopvisual]))
  fun `test exit visual`() {
    val keys = listOf("v", "<End>")
    val before = """
            Lorem Ipsum

            I found it in a ${c}legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I found it in a legendary lan${c}d
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, limitedValues = [OptionConstants.keymodel_stopselect]))
  fun `test exit select`() {
    val keys = listOf("gh", "<End>")
    val before = """
            Lorem Ipsum

            I found it in a ${c}legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I found it in a legendary lan${c}d
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test delete to the end`() {
    val keys = listOf("d", "<End>")
    val before = """
            Lorem Ipsum

            I found it in a leg${c}endary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I found it in a le${c}g
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @OptionTest(VimOption(TestOptionConstants.keymodel, doesntAffectTest = true))
  fun `test motion end with multiple code point grapheme cluster at the end`() {
    val keys = listOf("<End>")
    val before = """
            Lorem Ipsum

            I found it in ${c}a legendary land👩‍👩‍👧‍👧
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I found it in a legendary land${c}👩‍👩‍👧‍👧
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }
}

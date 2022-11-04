/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.shift

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class ShiftLeftTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test shift till new line`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("<W"), file)
    assertState(
      """
            A Discovery

            ${c}I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test shift left positions caret at first non-blank char`() {
    val file = """
      |A Discovery
      |
      |       I found it in a legendary l${c}and
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
    """.trimMargin()
    typeTextInFile(injector.parser.parseKeys("<<"), file)
    assertState(
      """
      |A Discovery

      |   ${c}I found it in a legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test shift left does not move caret with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    val file = """
      |A Discovery
      |
      |       I found it in a ${c}legendary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
    """.trimMargin()
    typeTextInFile(injector.parser.parseKeys("<<"), file)
    assertState(
      """
      |A Discovery

      |   I found it in a lege${c}ndary land
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test shift left positions caret at end of line with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    val file = """
      |A Discovery
      |
      |       I found it in a legendary la${c}nd
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
    """.trimMargin()
    typeTextInFile(injector.parser.parseKeys("<<"), file)
    assertState(
      """
      |A Discovery

      |   I found it in a legendary lan${c}d
      |       all rocks and lavender and tufted grass,
      |       where it was settled on some sodden sand
      |       hard by the torrent of a mountain pass.
      """.trimMargin()
    )
  }

  fun `test shift ctrl-D`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("i<C-D>"), file)
    assertState(
      """
            A Discovery

            I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
      """.trimIndent()
    )
  }
}

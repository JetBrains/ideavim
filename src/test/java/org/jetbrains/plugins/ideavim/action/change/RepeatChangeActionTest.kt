/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class RepeatChangeActionTest : VimTestCase() {
  @Test
  fun `test simple repeat`() {
    val keys = listOf("v2erXj^", ".")
    val before = """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
                Lorem Ipsum

                XXXXXXXXXXX dolor sit amet,
                ${c}XXXXXXXXXXX adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test simple repeat with dollar motion`() {
    val keys = listOf("v\$rXj^", ".")
    val before = """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
                Lorem Ipsum

                XXXXXXXXXXXXXXXXXXXXXXXXXXX
                ${c}XXXXXXXXXXXXXXXXXXXXXXXXXXX
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test repeat to line end`() {
    val keys = listOf("v2erXj\$b", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXX in a legendary land
                all rocks and lavender and tufted ${c}XXXXXX
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @VimBehaviorDiffers(description = "Different caret position")
  @Test
  fun `test repeat multiline`() {
    val keys = listOf("vjlrXj", ".")
    val before = """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
                A Discovery

                I XXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXXrocks and lavender and tufted grass,
                whe${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXX by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test count doesn't affect repeat`() {
    val keys = listOf("v2erXj^", "10.")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXX in a legendary land
                ${c}XXXXXXXXXXand lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test multicaret`() {
    val keys = listOf("v2erXj^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where ${c}it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXX in a legendary land
                ${c}XXXXXXXXXXand lavender and tufted grass,
                where XXXXXX settled on some sodden sand
                ${c}XXXXXXy the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test line motion`() {
    val keys = listOf("VrXj^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                ${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @VimBehaviorDiffers(description = "Wrong caret position")
  @Test
  fun `test line motion to end`() {
    val keys = listOf("VjrX2j^", ".")
    val before = """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
                Lorem Ipsum

                XXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXXXXXXXXXXXXXXXXXXXXXXXXX
                Sed in orci mauris.
                ${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @VimBehaviorDiffers(description = "Wrong caret position")
  @Test
  fun `test line motion shift`() {
    val keys = listOf("V3j<", ".")
    val before = """
                |Lorem Ipsum
                |
                |        ${c}Lorem ipsum dolor sit amet,
                |        consectetur adipiscing elit
                |        Sed in orci mauris.
                |        Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """
                |Lorem Ipsum
                |
                |${c}Lorem ipsum dolor sit amet,
                |consectetur adipiscing elit
                |Sed in orci mauris.
                |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @VimBehaviorDiffers(description = "Wrong caret position")
  @Test
  fun `test block motion`() {
    val keys = listOf("<C-V>jerXll", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
                A Discovery

                XXXound it in a legendary land
                XXX ${c}XXXks and lavender and tufted grass,
                wherXXXt was settled on some sodden sand
                hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @VimBehaviorDiffers(
    """
                Lorem Ipsum

                XXXXXnd it in a legendary land
                XXXXXocks and lavender and tufted grass,
                XXXXX it was settled on some sodden sand
                hard ${c}XXXXXe torrent of a mountain pass.

    """,
  )
  @Test
  fun `test block motion to end`() {
    val keys = listOf("<C-V>jjerXjl", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.

    """.trimIndent()
    val after = """
                A Discovery

                XXXXXnd it in a legendary land
                XXXXXocks and lavender and tufted grass,
                XXXXX it was settled on some sodden sand
                XXXXX${c}Xy the torrent of a mountain pass.

    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  @Test
  fun `test block with dollar motion`() {
    val keys = listOf("<C-V>j\$rXj^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land[additional characters]
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand[additional characters]
                hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                ${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test repeat with count`() {
    val keys = listOf("4x", "j", ".")
    val before = """
              Lorem Ipsum
  
              ${c}Lorem ipsum dolor sit amet,
              consectetur adipiscing elit
              Sed in orci mauris.
              Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
              Lorem Ipsum
  
              m ipsum dolor sit amet,
              ${c}ectetur adipiscing elit
              Sed in orci mauris.
              Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
    
        Three
        Two
        One
  """,
  )
  @Test
  fun `test redo register feature`() {
    doTest(
      listOf("dd", "dd", "dd", "\"1p", ".", "."),
      """
        One
        Two
        Three
      """.trimIndent(),
      """
        Three
        Two
        One
        
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-481"])
  fun `test dot after overwrite mode`() {
    configureByText(
      """
    /**
     * @param array ${'$'}arr_footers
     * @param array ${'$'}arr_totals_data
     * @param array ${'$'}arr_circulation_keys (Started beneath the 'a' of array on the line above; did Shift-R and typed 'array')
     * @param $c      ${'$'}arr_periods
     * @param       ${'$'}arr_options
     *
     * @return array
     */
    """.trimIndent()
    )
    typeText("Rarray<C-[>", "jgell.")
    assertState(
      """
    /**
     * @param array ${'$'}arr_footers
     * @param array ${'$'}arr_totals_data
     * @param array ${'$'}arr_circulation_keys (Started beneath the 'a' of array on the line above; did Shift-R and typed 'array')
     * @param array ${'$'}arr_periods
     * @param array ${'$'}arr_options
     *
     * @return array
     */
    """.trimIndent()
    )
  }
}

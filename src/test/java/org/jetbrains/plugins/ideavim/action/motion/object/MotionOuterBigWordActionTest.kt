/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("RemoveCurlyBracesFromTemplate", "SpellCheckingInspection")
class MotionOuterBigWordActionTest : VimTestCase() {
  @Test
  fun `test select outer WORD selects word and following whitespace`() {
    doTest(
      "vaW",
      "Lorem ip${c}sum dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD from beginning of word`() {
    doTest(
      "vaW",
      "Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD from end of word`() {
    doTest(
      "vaW",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD from inside whitespace selects word and not following whitespace`() {
    doTest(
      "vaW",
      "Lorem  ${c}  ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem${s}    ipsu${c}m${se} dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ipsum dolor sit amet,
      |
      |${s}
      |${c}
      |${se}
      |
      |
      |consectetur adipiscing elit
    """,
//    description = "The caret at the same offset as the selection end is an indication that there is an off-by-one error." +
//      "IdeaVim doesn't (currently) allow selecting the end of line char, so this inclusive range does not include the" +
//      "(exclusive) end of line char. Once IdeaVim handles this, we might have to fix things"
    description = "Not yet implemented"
  )
  @Test
  fun `test select empty line`() {
    doTest(
      "vaW",
      """
        |Lorem ipsum dolor sit amet,
        |
        |${c}
        |
        |
        |
        |
        |consectetur adipiscing elit
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet,
        |
        |${s}
        |
        |
        |
        |
        |consectetu${c}r${se} adipiscing elit
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select empty line wraps to next line`() {
    doTest(
      "vaW",
      """
        |Lorem ipsum dolor sit amet,
        |
        |${c}
        |consectetur adipiscing elit
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet,
        |
        |${s}
        |consectetu${c}r${se} adipiscing elit
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD skips following empty line`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem ipsum dolor sit amet,
        |
        |con${c}sectetur
        |
        |adipiscing elit
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet,
        |
        |${s}consectetur
        |
        |adipiscin${c}g${se} elit
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ipsum dolor sit amet,
      |
      |${s}
      |
      |${c}${se}consectetur adipiscing elit
    """,
  )
  @Test
  fun `test select empty line wraps to next line but does not wrap to following line`() {
    doTest(
      "vaW",
      """
        |Lorem ipsum dolor sit amet,
        |
        |${c}
        |
        |consectetur adipiscing elit
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet,
        |
        |${s}
        |
        |consectetu${c}r${se} adipiscing elit
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ipsum dolor sit amet,
      |
      |${s}
      |
      |
      |
      |
      |${c}
      |${se}
      |consectetur adipiscing elit
    """
  )
  @Test
  fun `test select multiple empty lines`() {
    doTest(
      "v3aW",
      """
        |Lorem ipsum dolor sit amet,
        |
        |${c}
        |
        |
        |
        |
        |
        |
        |consectetur adipiscing elit
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet,
        |
        |${s}
        |
        |
        |
        |
        |
        |
        |consectetur adipiscing eli${c}t${se}
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ipsum dolor sit amet,
      |
      |${s}....
      |${c}
      |${se}
      |
      |consectetur adipiscing elit
    """,
//    description = "The caret at the same offset as the selection end is an indication that there is an off-by-one error." +
//      "IdeaVim doesn't (currently) allow selecting the end of line char, so this inclusive range does not include the" +
//      "(exclusive) end of line char. Once IdeaVim handles this, we might have to fix things"
    description = "Not yet implemented"
  )
  @Test
  fun `test select blank line`() {
    doTest(
      "vaW",
      """
        |Lorem ipsum dolor sit amet,
        |
        |..${c}..
        |
        |
        |
        |consectetur adipiscing elit
      """.trimMargin().dotToSpace(),
      """
        |Lorem ipsum dolor sit amet,
        |
        |${s}....
        |
        |
        |
        |consectetu${c}r${se} adipiscing elit
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = "Lorem ${s}ipsum ${c}${se}dolor sit amet, consectetur adipiscing elit",
    description = "Text objects are implicitly inclusive. Vim adjusts the caret location for inclusive motions"
  )
  @Test
  fun `test select outer WORD with exclusive selection`() {
    doTest(
      "vaW",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands selection to whitespace after next WORD`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |con${c}sectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |${s}consectetur adipiscing${c} ${se}elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object starting from whitespace expands selection`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ipsum
        |
        |Lorem${c} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem${s} ipsum dolo${c}r${se} sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test text object with count expands selection`() {
    doTest(
      "v2aW",
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |con${c}sectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |${s}consectetur adipiscing${c} ${se}elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands selection to end of line`() {
    doTest(
      listOf("v2aW", "aW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |con${c}sectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |${s}consectetur adipiscing eli${c}t${se}
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands selection to whitespace at end of line`() {
    doTest(
      listOf("v2aW", "aW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |con${c}sectetur adipiscing elit........
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |${s}consectetur adipiscing elit.......${c}.${se}
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands across new line`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing el${c}it
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing${s} elit
        |Sed${c} ${se}in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands over whitespace following new line`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing el${c}it
        |    Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing${s} elit
        |    Se${c}d${se} in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands to empty line`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ip${c}sum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem${s} Ipsum
        |
        |Lore${c}m${se} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem${s} Ipsum
      |
      |${c}${se}
      |
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """
  )
  @Test
  fun `test repeated text object expands to multiple empty lines`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ip${c}sum
        |
        |
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem${s} Ipsum
        |
        |
        |
        |Lore${c}m${se} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands to cover whitespace on following blank line`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ip${c}sum
        |........
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem${s} Ipsum
        |........
        |Lore${c}m${se} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // TODO: Fix this bug
  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem${s} Ipsum
      |
      |........
      |${c}
      |${se}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """
  )
  @Test
  fun `test repeated text object expands to cover whitespace on following blank lines`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ip${c}sum
        |
        |........
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem${s} Ipsum
        |
        |........
        |
        |Lore${c}m${se} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands over non-word character`() {
    doTest(
      listOf("vaW", "aW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit ${s}amet, consectetur${c} ${se}adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD selects following whitespace up to punctuation`() {
    doTest(
      "vaW",
      "Lorem ipsu${c}m   ...dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum  ${c} ${se}...dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD with following non-word characters selects following WORD characters`() {
    doTest(
      "vaW",
      "Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit",
      "Lorem ipsum dolor sit ${s}amet,${c} ${se}consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD at end of line selects preceding whitespace`() {
    doTest(
      "vaW",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing e${c}lit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing${s} eli${c}t${se}
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD at end of file selects preceding whitespace`() {
    doTest(
      "vaW",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet eg${c}estas
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet${s} egesta${c}s${se}
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD on only word on line does not select leading whitespace`() {
    doTest("vaW", "    Lor${c}em", "    ${s}Lore${c}m${se}", Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test select outer WORD on last word on line selects trailing whitespace`() {
    doTest("vaW", "    Lor${c}em    ", "    ${s}Lorem   ${c} ${se}", Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test select outer WORD with existing left-to-right selection selects rest of word and following whitespace`() {
    doTest(
      listOf("v", "l", "aW"),
      "Lo${c}rem    ipsum",
      "Lo${s}rem   ${c} ${se}ipsum",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD with existing left-to-right selection selects rest of word and trailing whitespace at end of line`() {
    doTest(
      listOf("v", "l", "aW"),
      "Lo${c}rem    ",
      "Lo${s}rem   ${c} ${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD with existing left-to-right selection in whitespace selects rest of whitespace and following word`() {
    doTest(
      listOf("v", "l", "aW"),
      "Lorem   ${c}   ipsum dolor sit amet",
      "Lorem   ${s}   ipsu${c}m${se} dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD with existing right-to-left selection selects rest of word and preceding whitespace`() {
    doTest(
      listOf("v", "h", "aW"),
      "Lorem   ip${c}sum   dolor sit amet",
      "Lorem${s}${c}   ips${se}um   dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD with existing right-to-left selection with caret at start of word selects previous word`() {
    doTest(
      listOf("v", "h", "aW"),
      "Lorem   i${c}psum   dolor sit amet",
      "${s}${c}Lorem   ip${se}sum   dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD with existing right-to-left selection selects rest of word and leading whitespace at start of line`() {
    doTest(
      listOf("v", "h", "aW"),
      "    Lo${c}rem ipsum dolor sit amet",
      "${s}${c}    Lor${se}em ipsum dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD with existing right-to-left selection on only word on line selects rest of word and leading whitespace`() {
    doTest(
      listOf("v", "h", "aW"),
      "    Lo${c}rem",
      "${s}${c}    Lor${se}em",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer WORD with existing right-to-left selection in whitespace selects rest of whitespace and preceding word`() {
    doTest(
      listOf("v", "h", "aW"),
      "Lorem ipsum   ${c}   dolor sit amet",
      "Lorem ${s}${c}ipsum    ${se}  dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer WORDs selects following whitespace`() {
    doTest(
      "v2aW",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum dolor${c} ${se}sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer WORDs with non-word characters`() {
    doTest(
      "v2aW",
      "Lorem ipsu${c}m -- dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum --${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer WORDs with non-word characters 2`() {
    doTest(
      "v2aW",
      "Lorem ipsu${c}m --dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum --dolor${c} ${se}sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer words starting in whitespace`() {
    doTest(
      "v3aW",
      "Lorem  ${c}  ipsum --dolor sit amet, consectetur adipiscing elit",
      "Lorem${s}    ipsum --dolor si${c}t${se} amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer WORDs crosses end of line`() {
    doTest(
      "v2aW",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing e${c}lit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing ${s}elit
        |Sed${c} ${se}in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, description = "Wrong caret position, but in real neovim works fine")
  @Test
  fun `test on last dot`() {
    doTest(
      "<aW",
      """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      ${c}hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test past end in visual`() {
    doTest(
      "v\$aW",
      """
      I found it in a ${c}legendary land
      }
      """.trimIndent(),
      """
      I found it in a ${s}legendary land
      $c}$se
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}

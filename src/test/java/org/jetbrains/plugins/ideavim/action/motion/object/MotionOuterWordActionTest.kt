/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection", "RemoveCurlyBracesFromTemplate")
class MotionOuterWordActionTest : VimTestCase() {
  @Test
  fun `test select outer word selects word and following whitespace`() {
    doTest(
      "vaw",
      "Lorem ip${c}sum dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word from beginning of word`() {
    doTest(
      "vaw",
      "Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word from end of word`() {
    doTest(
      "vaw",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word from inside whitespace selects word and not following whitespace`() {
    doTest(
      "vaw",
      "Lorem  ${c}  ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem${s}    ipsu${c}m${se} dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = "Lorem ${s}ipsum ${c}${se}dolor sit amet, consectetur adipiscing elit",
    description = "Text objects are implicitly inclusive. Vim adjusts the caret location for inclusive motions"
  )
  @Test
  fun `test select outer word with exclusive selection`() {
    doTest(
      "vaw",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands selection to whitespace after next word`() {
    doTest(
      listOf("vaw", "aw"),
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
      listOf("vaw", "aw"),
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
      "v2aw",
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
      listOf("v2aw", "aw"),
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
      listOf("v2aw", "aw"),
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

  // TODO: Fix this bug
  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem Ipsum
      |
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing${s} elit
      |Sed${c} ${se}in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """
  )
  @Test
  fun `test repeated text object expands across new line`() {
    doTest(
      listOf("vaw", "aw"),
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
        |Se${c}d${se} in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands over whitespace following new line`() {
    doTest(
      listOf("vaw", "aw"),
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
      listOf("vaw", "aw"),
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
      listOf("vaw", "aw"),
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
      listOf("vaw", "aw"),
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
      listOf("vaw", "aw"),
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

  // TODO: Fix this bug
  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem Ipsum
      |
      |Lorem ipsum dolor sit${s} amet,${c} ${se}consectetur adipiscing elit
      |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
    """
  )
  @Test
  fun `test repeated text object expands over non-word character`() {
    doTest(
      listOf("vaw", "aw"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit${s} amet${c},${se} consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word selects following whitespace up to punctuation`() {
    doTest(
      "vaw",
      "Lorem ipsu${c}m   ...dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum  ${c} ${se}...dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word with following non-word characters selects preceding whitespace`() {
    doTest(
      "vaw",
      "Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit",
      "Lorem ipsum dolor sit${s} ame${c}t${se}, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word at end of line selects preceding whitespace`() {
    doTest(
      "vaw",
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
  fun `test select outer word at end of file selects preceding whitespace`() {
    doTest(
      "vaw",
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
  fun `test select outer word on only word on line does not select leading whitespace`() {
    doTest("vaw", "    Lor${c}em", "    ${s}Lore${c}m${se}", Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test select outer word on last word on line selects trailing whitespace`() {
    doTest("vaw", "    Lor${c}em    ", "    ${s}Lorem   ${c} ${se}", Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test select outer word with existing left-to-right selection selects rest of word and following whitespace`() {
    doTest(
      listOf("v", "l", "aw"),
      "Lo${c}rem    ipsum",
      "Lo${s}rem   ${c} ${se}ipsum",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word with existing left-to-right selection selects rest of word and trailing whitespace at end of line`() {
    doTest(
      listOf("v", "l", "aw"),
      "Lo${c}rem    ",
      "Lo${s}rem   ${c} ${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word with existing left-to-right selection in whitespace selects rest of whitespace and following word`() {
    doTest(
      listOf("v", "l", "aw"),
      "Lorem   ${c}   ipsum dolor sit amet",
      "Lorem   ${s}   ipsu${c}m${se} dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word with existing right-to-left selection selects rest of word and preceding whitespace`() {
    doTest(
      listOf("v", "h", "aw"),
      "Lorem   ip${c}sum   dolor sit amet",
      "Lorem${s}${c}   ips${se}um   dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word with existing right-to-left selection with caret at start of word selects previous word`() {
    doTest(
      listOf("v", "h", "aw"),
      "Lorem   i${c}psum   dolor sit amet",
      "${s}${c}Lorem   ip${se}sum   dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word with existing right-to-left selection selects rest of word and leading whitespace at start of line`() {
    doTest(
      listOf("v", "h", "aw"),
      "    Lo${c}rem ipsum dolor sit amet",
      "${s}${c}    Lor${se}em ipsum dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word with existing right-to-left selection on only word on line selects rest of word and leading whitespace`() {
    doTest(
      listOf("v", "h", "aw"),
      "    Lo${c}rem",
      "${s}${c}    Lor${se}em",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select outer word with existing right-to-left selection in whitespace selects rest of whitespace and preceding word`() {
    doTest(
      listOf("v", "h", "aw"),
      "Lorem ipsum   ${c}   dolor sit amet",
      "Lorem ${s}${c}ipsum    ${se}  dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer words selects following whitespace`() {
    doTest(
      "v2aw",
      "Lorem ipsu${c}m dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum dolor${c} ${se}sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer words with non-word characters`() {
    doTest(
      "v2aw",
      "Lorem ipsu${c}m -- dolor sit amet, consectetur adipiscing elit",
      "Lorem ${s}ipsum --${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer words with no following whitespace selects preceding whitespace`() {
    doTest(
      "v2aw",
      "Lorem ipsu${c}m --dolor sit amet, consectetur adipiscing elit",
      "Lorem${s} ipsum -${c}-${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    "Lorem${s}    ipsum --dolor${c} ${se}sit amet, consectetur adipiscing elit",
    description = "First aw should select whitespace+'ipsum' " +
      "second should select whitespace+'--' " +
      "third should select 'dolor' and following whitespace",
  )
  @Test
  fun `test select multiple outer words starting in whitespace`() {
    doTest(
      "v3aw",
      "Lorem  ${c}  ipsum --dolor sit amet, consectetur adipiscing elit",
      "Lorem${s}    ipsum --dolo${c}r${se} sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer words with no following whitespace selects preceding whitespace 2`() {
    // Implementation bug: caret placed anywhere other than last character would not select preceding whitespace
    doTest(
      "v2aw",
      "Lorem ip${c}sum --dolor sit amet, consectetur adipiscing elit",
      "Lorem${s} ipsum -${c}-${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple outer words crosses end of line`() {
    doTest(
      "v2aw",
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
}

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
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection", "RemoveCurlyBracesFromTemplate")
class MotionInnerWordActionTest : VimTestCase() {
  @Test
  fun `test select word from beginning of word`() {
    doTest(
      "viw",
      "${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lore${c}m${se} ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word from middle of word`() {
    doTest(
      "viw",
      "Lo${c}rem ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lore${c}m${se} ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word from end of word`() {
    doTest(
      "viw",
      "Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lore${c}m${se} ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word from inside whitespace selects whitespace`() {
    doTest(
      "viw",
      "Lorem  ${c}  ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem${s}   ${c} ${se}ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word from whitespace at start of line`() {
    doTest(
      "viw",
      "  ${c}    Lorem ipsum",
      "${s}     ${c} ${se}Lorem ipsum",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word from whitespace at end of line`() {
    doTest(
      "viw",
      "Lorem ipsum   ${c}   ",
      "Lorem ipsum${s}     ${c} ${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word selects up to punctuation`() {
    doTest(
      "viw",
      "Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit",
      "Lorem ipsum dolor sit ${s}ame${c}t${se}, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word with existing left-to-right selection selects rest of word`() {
    doTest(
      listOf("v", "l", "iw"),
      "Lorem ipsum dolor sit amet, con${c}sectetur adipiscing elit",
      "Lorem ipsum dolor sit amet, con${s}sectetu${c}r${se} adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word with existing left-to-right selection in whitespace selects rest of whitespace`() {
    doTest(
      listOf("v", "l", "iw"),
      "Lorem  ${c}    ipsum dolor sit amet",
      "Lorem  ${s}   ${c} ${se}ipsum dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // TODO: Fix this bug
  @VimBehaviorDiffers(originalVimAfter = "Lorem ipsum dolor sit amet, ${s}${c}consectet${se}ur adipiscing elit")
  @Test
  fun `test select word with existing right-to-left selection selects start of word`() {
    doTest(
      listOf("v", "h", "iw"),
      "Lorem ipsum dolor sit amet, consecte${c}tur adipiscing elit",
      "Lorem ipsum dolor sit amet,${s}${c} consectet${se}ur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select word with existing right-to-left selection in whitespace selects rest of whitespace`() {
    doTest(
      listOf("v", "h", "iw"),
      "Lorem    ${c}  ipsum dolor sit amet",
      "Lorem${s}${c}     ${se} ipsum dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = "${s}Lorem${c}${se} ipsum dolor sit amet, consectetur adipiscing elit",
    description = "Text objects are implicitly inclusive, because they set the selection." +
      "Vim modifies the caret offset of inclusive motions when in exclusive selection mode. " +
      "Fix this when IdeaVim also handles inclusive motions in exclusive selection mode."
  )
  @Test
  fun `test select word with exclusive selection`() {
    doTest(
      "viw",
      "Lo${c}rem ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lore${c}${se}m ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    ) {
      enterCommand("set selection=exclusive")
    }
  }

  @Test
  fun `test repeated text object expands selection to following whitespace`() {
    doTest(
      listOf("viw", "iw"),
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
        |${s}consectetur${c} ${se}adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands selection to word following whitespace`() {
    doTest(
      listOf("viw", "iw", "iw"),
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
        |${s}consectetur adipiscin${c}g${se} elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test text object with count expands selection`() {
    doTest(
      "v3iw",
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
        |${s}consectetur adipiscin${c}g${se} elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object does not expand selection from single character selection`() {
    // Surprisingly, this is correct Vim behaviour! It does not expand selection from a single character selection
    doTest(
      listOf("viw", "iw"),
      """
        |Lorem Ipsum
        |
        |Lore ${c}m ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lore ${s}${c}m${se} ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object does not expand selection from current single whitespace`() {
    // Surprisingly, this is correct Vim behaviour! It does not expand selection from a single whitespace selection
    doTest(
      listOf("viw", "iw"),
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
        |Lorem${s}${c} ${se}ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test text object with count will expand selection from current single whitespace`() {
    // `viwiw` on a single character doesn't expand selection, but `v2iw` does. Go figure
    doTest(
      listOf("v3iw"),
      """
        |Lorem Ipsum
        |
        |Lore ${c}m ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lore ${s}m ipsu${c}m${se} dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands selection to end of line`() {
    doTest(
      listOf("v2iw", "iw"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adi${c}piscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur ${s}adipiscing eli${c}t${se}
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // TODO: Fix this bug
  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem Ipsum
      |
      |Lorem ipsum dolor sit amet,
      |consectetur ${s}adipiscing elit.......${c}.${se}
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "Caret placement is incorrect, but the selection is correct - it should not select the final newline char"
  )
  @Test
  fun `test repeated text object expands selection to whitespace at end of line`() {
    doTest(
      listOf("v3iw", "iw"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adi${c}piscing elit........
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur ${s}adipiscing elit........${c}${se}
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
      |consectetur adipiscing ${s}elit
      |Se${c}d${se} in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "IdeaVim expands to include newline character"
  )
  @Test
  fun `test repeated text object expands across new line`() {
    doTest(
      listOf("viw", "iw", "iw"),  // TODO: This should be just listOf("viw", "iw")
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing e${c}lit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing ${s}elit
        |Se${c}d${se} in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands to whitespace following new line`() {
    doTest(
      listOf("viw", "iw"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing e${c}lit
        |    Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing ${s}elit
        |   ${c} ${se}Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ${s}Ipsum
      |
      |${c}L${se}orem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "Vim's behaviour is weird. Makes no sense that it selects the first character of the word. " +
      "Possibly a bug in Vim: https://github.com/vim/vim/issues/16514 " +
      "Unclear what the correct behaviour should be",
    shouldBeFixed = false
  )
  @Test
  fun `test repeated text object expands to empty line`() {
    doTest(
      listOf("viw", "iw"),
      """
        |Lorem Ip${c}sum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ${s}Ipsum
        |${c}${se}
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ${s}Ipsum
      |
      |${c}
      |${se}Lorem ipsum dolor sit amet,
    """,
    description = "Off by one because IdeaVim does not allow selecting a newline char"
  )
  @Test
  fun `test repeated text object expands to multiple empty lines`() {
    doTest(
      listOf("viw", "iw"),
      """
        |Lorem Ip${c}sum
        |
        |
        |Lorem ipsum dolor sit amet,
      """.trimMargin(),
      """
        |Lorem ${s}Ipsum
        |
        |${c}${se}
        |Lorem ipsum dolor sit amet,
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ${s}Ipsum
      |.......${c}.${se}
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "Caret placement is incorrect, but selection is correct. It should not select trailing new line char"
  )
  @Test
  fun `test repeated text object expands to whitespace on following blank line`() {
    doTest(
      listOf("viw", "iw"),
      """
        |Lorem Ip${c}sum
        |........
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem ${s}Ipsum
        |........${c}${se}
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ${s}Ipsum
      |
      |${c}.${se}.......
      |
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "Vim's behaviour is weird. Makes no sense that it selects the first character of the word. " +
      "Possibly a bug in Vim: https://github.com/vim/vim/issues/16514 " +
      "Unclear what the correct behaviour should be",
    shouldBeFixed = false
  )
  @Test
  fun `test repeated text object expands to whitespace on following blank lines`() {
    doTest(
      listOf("viw", "iw"),
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
        |Lorem ${s}Ipsum
        |
        |........
        |${c}${se}
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands to include non-word character`() {
    doTest(
      listOf("viw", "iw"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit ${s}amet${c},${se} consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands to include whitespace after non-word character`() {
    doTest(
      listOf("viw", "iw", "iw"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit a${c}met, consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit ${s}amet,${c} ${se}consectetur adipiscing elit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple words selects includes whitespace between words in count`() {
    doTest(
      "v2iw",
      "${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lorem${c} ${se}ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple words selects includes whitespace between words in count 2`() {
    doTest(
      "v3iw",
      "${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lorem ipsu${c}m${se} dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple words starting in whitespace`() {
    doTest(
      "v3iw",
      "Lorem  ${c}  ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem${s}    ipsum${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // TODO: Fix this bug
  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ipsum dolor sit amet, consectetur adipiscing ${s}elit
      |Sed${c} ${se}in orci mauris. Cras id tellus in ex imperdiet egestas.
    """,
    description = "Unclear why this fails. IdeaVim seems to treat the new line char as a word"
  )
  @Test
  fun `test select multiple words wraps to next line`() {
    doTest(
      "v3iw",
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing e${c}lit
        |Sed in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing ${s}elit
        |Se${c}d${se} in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test empty text`() {
    doTest(
      "viw",
      "",
      "",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}

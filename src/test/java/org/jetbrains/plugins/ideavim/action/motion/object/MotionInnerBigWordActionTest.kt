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

@Suppress("RemoveCurlyBracesFromTemplate", "SpellCheckingInspection")
class MotionInnerBigWordActionTest : VimTestCase() {
  @Test
  fun `test select WORD from beginning of word`() {
    doTest(
      "viW",
      "${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lore${c}m${se} ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from middle of word`() {
    doTest(
      "viW",
      "Lo${c}rem ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lore${c}m${se} ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from end of word`() {
    doTest(
      "viW",
      "Lore${c}m ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}Lore${c}m${se} ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from inside whitespace selects whitespace`() {
    doTest(
      "viW",
      "Lorem  ${c}  ...ipsum dolor sit amet, consectetur adipiscing elit",
      "Lorem${s}   ${c} ${se}...ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at start of line`() {
    doTest(
      "viW",
      "  ${c}    ...Lorem ipsum",
      "${s}     ${c} ${se}...Lorem ipsum",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at start of line with multiple lines`() {
    doTest(
      "viW",
      """
        |Lorem Ipsum
        |
        |    ${c}    Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |${s}       ${c} ${se}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at start of line with multiple lines 2`() {
    doTest(
      "viW",
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet
        |    ${c}    consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet
        |${s}       ${c} ${se}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at end of line`() {
    doTest(
      "viW",
      "Lorem ipsum...   ${c}   ",
      "Lorem ipsum...${s}     ${c} ${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at end of line with multiple lines`() {
    doTest(
      "viW",
      """
        |Lorem Ipsum....${c}....
        |
        |Lorem ipsum dolor sit amet
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum${s}.......${c}.${se}
        |
        |Lorem ipsum dolor sit amet
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at end of line with multiple lines 2`() {
    doTest(
      "viW",
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet....${c}....
        |....consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet${s}.......${c}.${se}
        |....consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD ignores punctuation`() {
    doTest(
      "viW",
      "Lorem ipsum dolor sit a${c}met,consectetur adipiscing elit",
      "Lorem ipsum dolor sit ${s}amet,consectetu${c}r${se} adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD with existing left-to-right selection selects rest of word`() {
    doTest(
      listOf("v", "l", "iW"),
      "Lorem ipsum dolor sit amet, con${c}sectetur adipiscing elit",
      "Lorem ipsum dolor sit amet, con${s}sectetu${c}r${se} adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD with existing left-to-right selection in whitespace selects rest of whitespace`() {
    doTest(
      listOf("v", "l", "iW"),
      "Lorem  ${c}    ipsum dolor sit amet",
      "Lorem  ${s}   ${c} ${se}ipsum dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD with existing left-to-right selection in whitespace selects trailing whitespace at end of file`() {
    doTest(
      listOf("v", "l", "iW"),
      "Lorem ipsum dolor sit amet  ${c}      ",
      "Lorem ipsum dolor sit amet  ${s}     ${c} ${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at end of line with multiple lines and existing left-to-right selection`() {
    doTest(
      listOf("v", "l", "iW"),
      """
        |Lorem Ipsum...${c}.....
        |
        |Lorem ipsum dolor sit amet
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum...${s}....${c}.${se}
        |
        |Lorem ipsum dolor sit amet
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at end of line with multiple lines and existing left-to-right selection 2`() {
    doTest(
      listOf("v", "l", "iW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet...${c}.....
        |    consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet...${s}....${c}.${se}
        |    consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD with existing right-to-left selection selects start of word`() {
    doTest(
      listOf("v", "h", "iW"),
      "Lorem ipsum dolor sit amet, consecte${c}tur adipiscing elit",
      "Lorem ipsum dolor sit amet, ${s}${c}consectet${se}ur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD with existing right-to-left selection in whitespace selects rest of whitespace`() {
    doTest(
      listOf("v", "h", "iW"),
      "Lorem    ${c}  ipsum dolor sit amet",
      "Lorem${s}${c}     ${se} ipsum dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD with existing right-to-left selection in whitespace selects leading whitespace at start of file`() {
    doTest(
      listOf("v", "h", "iW"),
      "   ${c}   Lorem ipsum dolor sit amet",
      "${s}${c}    ${se}  Lorem ipsum dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at start of line with multiple lines and existing right-to-left selection`() {
    doTest(
      listOf("v", "h", "iW"),
      """
        |Lorem Ipsum
        |
        |    ${c}    Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |${s}${c}     ${se}   Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at start of line with multiple lines and existing right-to-left-selection 2`() {
    doTest(
      listOf("v", "h", "iW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet
        |    ${c}    consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet
        |${s}${c}     ${se}   consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from word at start of line with existing right-to-left selection`() {
    doTest(
      listOf("v", "h", "iW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |c${c}onsectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit ${s}${c}amet,
        |co${se}nsectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select WORD from whitespace at start of line with existing right-to-left selection`() {
    doTest(
      listOf("v", "h", "iW"),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        | ${c}   consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |Lorem ipsum dolor sit ${s}${c}amet,
        |  ${se}  consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ipsum dolor sit amet,
      |
      |${s}${c}
      |${se}
      |
      |consectetur adipiscing elit
    """,
    description = "The caret and selection should not be at the same offset. This indicates that IdeaVim is " +
      "shortening the selection range to (incorrectly) avoid selecting the end of line char. Once IdeaVim allows " +
      "this, both caret and selection end offset will be incorrect, indicating an off-by-ine error somewhere, " +
      "which will need fixing." +
      "Fix when IdeaVim supports selecting end of line char"
  )
  @Test
  fun `test select empty line`() {
    doTest(
      "viW",
      """
        |Lorem ipsum dolor sit amet,
        |
        |${c}
        |
        |
        |consectetur adipiscing elit
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet,
        |
        |${s}
        |${c}${se}
        |
        |consectetur adipiscing elit
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple empty lines`() {
    // I don't understand the logic of this. I would expect v3iw to select 3 lines, but instead it selects 5.
    // And v4iw selects 7, v5iw selects 9. But this is how Vim behaves, and it's working and the other tests are working
    // too. Let's not question it too hard.
    doTest(
      "v3iW",
      """
        |Lorem ipsum dolor sit amet,
        |
        |${c}
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
        |${c}${se}
        |
        |consectetur adipiscing elit
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select blank line`() {
    doTest(
      "viW",
      """
        |Lorem ipsum dolor sit amet,
        |
        |..${c}..
        |
        |
        |consectetur adipiscing elit
      """.trimMargin().dotToSpace(),
      """
        |Lorem ipsum dolor sit amet,
        |
        |${s}...${c}.${se}
        |
        |
        |consectetur adipiscing elit
      """.trimMargin().dotToSpace(),
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
  fun `test select WORD with exclusive selection`() {
    doTest(
      "viW",
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
      listOf("viW", "iW"),
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
  fun `test repeated text object expands selection to WORD following whitespace`() {
    doTest(
      listOf("viW", "iW", "iW"),
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
      "v3iW",
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
      listOf("viW", "iW"),
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
      listOf("viW", "iW"),
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
      listOf("v3iW"),
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
      listOf("v2iW", "iW"),
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

  @Test
  fun `test repeated text object expands selection to whitespace at end of line`() {
    doTest(
      listOf("v3iW", "iW"),
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
        |consectetur ${s}adipiscing elit.......${c}.${se}
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands across new line`() {
    doTest(
      listOf("viW", "iW"),
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
      listOf("viW", "iW"),
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

  @Test
  fun `test repeated text object expands to empty line`() {
    // Well. This behaviour is weird, and looks like a bug, but it matches Vim's behaviour.
    // There's an explanation of the behaviour in VimSearchHelperBase.findWordUnderCursor. Basically, Vim moves forward
    // over an empty line, but when moving back, stops at the start of a line. This puts us off-by-one, but in the same
    // way that Vim is off-by-one!
    // See https://github.com/vim/vim/issues/16514
    doTest(
      listOf("viW", "iW"),
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
        |
        |${c}L${se}orem ipsum dolor sit amet,
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
      listOf("viW", "iW"),
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

  @Test
  fun `test repeated text object expands to whitespace on following blank line`() {
    doTest(
      listOf("viW", "iW"),
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
        |.......${c}.${se}
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
      listOf("viW", "iW"),
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
        |.......${c}.${se}
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin().dotToSpace(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated text object expands to include whitespace after non-word character`() {
    doTest(
      listOf("viW", "iW"),
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
  fun `test select multiple WORDs selects includes whitespace between words in count`() {
    doTest(
      "v2iW",
      "...Lo${c}rem... ipsum dolor sit amet, consectetur adipiscing elit",
      "${s}...Lorem...${c} ${se}ipsum dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple WORDs selects includes whitespace between words in count 2`() {
    doTest(
      "v3iW",
      "...L${c}orem... ipsum... dolor sit amet, consectetur adipiscing elit",
      "${s}...Lorem... ipsum..${c}.${se} dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test select multiple WORDs starting in whitespace`() {
    doTest(
      "v3iW",
      "Lorem  ${c}  ...ipsum... dolor sit amet, consectetur adipiscing elit",
      "Lorem${s}    ...ipsum...${c} ${se}dolor sit amet, consectetur adipiscing elit",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

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
        |Sed${c} ${se}in orci mauris. Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test at last dot`() {
    doTest(
      "diW",
      """
          Lorem ipsum dolor sit amet,
          consectetur adipiscing elit
          Sed in orci mauris.
          hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
          Lorem ipsum dolor sit amet,
          consectetur adipiscing elit
          Sed in orci mauris.
          hard by the torrent of a mountain$c 
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}

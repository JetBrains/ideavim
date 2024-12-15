/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.google.common.collect.Lists
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

@Suppress("SpellCheckingInspection")
class MarkTest : VimTestCase() {
  // |m|
  @Test
  fun testLocalMark() {
    typeTextInFile(
      injector.parser.parseKeys("ma"),
      """    foo
    ba<caret>r
    baz
""",
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'a')
    assertNotNull<Any>(mark)
    kotlin.test.assertEquals(1, mark.line)
    kotlin.test.assertEquals(6, mark.col)
  }

  // |m|
  @Test
  fun testGlobalMark() {
    typeTextInFile(
      injector.parser.parseKeys("mG"),
      """    foo
    ba<caret>r
    baz
""",
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'G')
    assertNotNull<Any>(mark)
    kotlin.test.assertEquals(1, mark.line)
    kotlin.test.assertEquals(6, mark.col)
  }

  // |m|
  @Test
  fun testMarkIsDeletedWhenLineIsDeleted() {
    typeTextInFile(
      injector.parser.parseKeys("mx" + "dd"),
      """    foo
    ba<caret>r
    baz
""",
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'x')
    kotlin.test.assertNull(mark)
  }

  // |m|
  @Test
  fun testMarkIsNotDeletedWhenLineIsOneCharAndReplaced() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "r1"),
      """
     foo
     <caret>0
     bar
     
      """.trimIndent(),
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'a')
    assertNotNull<Any>(mark)
  }

  // |m|
  @Test
  fun testMarkIsNotDeletedWhenLineIsChanged() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "cc"),
      """    foo
    ba<caret>r
    baz
""",
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'a')
    assertNotNull<Any>(mark)
  }

  // |m|
  @Test
  fun testMarkIsMovedUpWhenLinesArePartiallyDeletedAbove() {
    typeTextInFile(
      injector.parser.parseKeys("mx" + "2k" + "dd" + "0dw"),
      """    foo
    bar
    ba<caret>z
""",
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'x')
    assertNotNull<Any>(mark)
    kotlin.test.assertEquals(1, mark.line)
    kotlin.test.assertEquals(6, mark.col)
  }

  // |m|
  @Test
  fun testMarkIsMovedUpWhenLinesAreDeletedAbove() {
    typeTextInFile(
      injector.parser.parseKeys("mx" + "2k" + "2dd"),
      """    foo
    bar
    ba<caret>z
""",
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'x')
    assertNotNull<Any>(mark)
    kotlin.test.assertEquals(0, mark.line)
    kotlin.test.assertEquals(6, mark.col)
  }

  // |m|
  @Test
  fun testMarkIsMovedDownWhenLinesAreInsertedAbove() {
    typeTextInFile(
      injector.parser.parseKeys("mY" + "Obiff"),
      """
     foo
     ba<caret>r
     baz
     
      """.trimIndent(),
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'Y')
    assertNotNull<Any>(mark)
    kotlin.test.assertEquals(2, mark.line)

    // Currently broken, needs investigation
    // Because of some reason system mark is recreated. As we're on a different column at this moment, this breaks test
//      assertEquals(2, mark.col)
  }

  // |m|
  @Test
  fun testMarkIsMovedDownWhenLinesAreInsertedAboveWithIndentation() {
    typeTextInFile(
      injector.parser.parseKeys("mY" + "Obiff"),
      """    foo
    ba<caret>r
    baz
""",
    )
    val vimEditor: VimEditor = IjVimEditor(fixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'Y')
    assertNotNull<Any>(mark)
    kotlin.test.assertEquals(2, mark.line)

    // Currently broken, needs investigation
    // Because of some reason system mark is recreated. As we're on a different column at this moment, this breaks test
//      assertEquals(6, mark.col)
  }

  // |m| |`|
  @Test
  fun testMarkAndJumpToMark() {
    typeTextInFile(
      injector.parser.parseKeys("6l" + "mZ" + "G$" + "`Z"),
      """    foo
    bar
    baz
""",
    )
    assertOffset(6)
  }

  // |m| |'|
  @Test
  fun testMarkAndJumpToMarkLeadingSpace() {
    typeTextInFile(
      injector.parser.parseKeys("6l" + "mb" + "G$" + "'b"),
      """    foo
    bar
    baz
""",
    )
    assertOffset(4)
  }

  // |m| |`|
  @Test
  fun testDeleteBacktickMotionIsCharacterWise() {
    typeTextInFile(
      injector.parser.parseKeys("mk" + "kh" + "d`k"),
      """    abcd
    efgh
    ij<caret>kl
    mnop
""",
    )
    assertState(
      """    abcd
    ekl
    mnop
""",
    )
  }

  // |m| |`|
  @Test
  fun testDeleteSingleQuoteMotionIsLineWise() {
    typeTextInFile(
      injector.parser.parseKeys("mk" + "kh" + "d'k"),
      """    abcd
    efgh
    ij<caret>kl
    mnop
""",
    )
    assertState(
      """    abcd
    mnop
""",
    )
  }

  // VIM-43 |i| |`.|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  @Test
  fun testGotoLastChangePosition() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "hello " + "<Esc>" + "gg" + "`."),
      """
                one two
                <caret>hello world
                three four
                
      """.trimIndent(),
    )
    assertOffset(13)
  }

  // VIM-43 |p| |`.|
  @Test
  fun testGotoLastPutPosition() {
    typeTextInFile(
      injector.parser.parseKeys("yy" + "p" + "gg" + "`."),
      """
     one two
     <caret>three
     four five
     
      """.trimIndent(),
    )
    assertOffset(14)
  }

  // VIM-3731 |m| |[`|
  @Test
  fun testGotoPreviousMark() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "jmb" + "wmc" + "[`"),
      """
      one two
      <caret>three
      four five
     
      """.trimIndent(),
    )
    assertOffset(14)
  }

  // VIM-3731 |m| |[`|
  @Test
  fun testGotoPreviousMarkIgnoresPlacingOrder() {
    typeTextInFile(
      injector.parser.parseKeys("mb" + "kma" + "jwmc" + "[`"),
      """
      one two
      three
      <caret>four five
     
      """.trimIndent(),
    )
    assertOffset(14)
  }

  // VIM-3731 |m| |[`|
  @Test
  fun testGotoPreviousMarkMultipleMarksOnSamePosition() {
    typeTextInFile(
      injector.parser.parseKeys("mb" + "kma" + "jwmcmd" + "[`"),
      """
      one two
      three
      <caret>four five
     
      """.trimIndent(),
    )
    assertOffset(14)
  }

  // VIM-3731 |m| |[`|
  @Test
  fun testGotoPreviousMarkWithCount() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "jmb" + "wmc" + "2[`"),
      """
      one two
      <caret>three
      four five
     
      """.trimIndent(),
    )
    assertOffset(8)
  }

  // VIM-3731 |m| |[`|
  @Test
  fun testGotoPreviousMarkWithExcessiveCount() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "jmb" + "wmc" + "5[`"),
      """
      one two
      <caret>three
      four five
     
      """.trimIndent(),
    )
    assertOffset(8)
  }

  // VIM-3731 |m| |[`|
  @Test
  fun testGotoPreviousMarkBeforeFirstMarkDoesNothing() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "jmb" + "wmc" + "ggw"+ "[`"),
      """
      one two
      <caret>three
      four five
     
      """.trimIndent(),
    )
    assertOffset(4)
  }

  // VIM-3731 |m| |]`|
  @Test
  fun testGotoNextMark() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "jmb" + "wmc" + "gg" + "]`"),
      """
      one two
      <caret>three
      four five
     
      """.trimIndent(),
    )
    assertOffset(8)
  }

  // VIM-3731 |m| |]`|
  @Test
  fun testGotoNextMarkIgnoresPlacingOrder() {
    typeTextInFile(
      injector.parser.parseKeys("mb" + "kma" + "jwmc" + "gg" + "]`"),
      """
      one two
      three
      <caret>four five
     
      """.trimIndent(),
    )
    assertOffset(8)
  }

  // VIM-3731 |m| |]`|
  @Test
  fun testGotoNextMarkMultipleMarksOnSamePosition() {
    typeTextInFile(
      injector.parser.parseKeys("mbmd" + "kma" + "jwmc" + "ggjj" + "]`"),
      """
      one two
      three
      <caret>four five
     
      """.trimIndent(),
    )
    assertOffset(19)
  }

  // VIM-3731 |m| |]`|
  @Test
  fun testGotoNextMarkWithCount() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "jmb" + "wmc" + "gg" + "2]`"),
      """
      one two
      <caret>three
      four five
     
      """.trimIndent(),
    )
    assertOffset(14)
  }

  // VIM-3731 |m| |]`|
  @Test
  fun testGotoNextMarkWithExcessiveCount() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "jmb" + "wmc" + "gg" + "5]`"),
      """
      one two
      <caret>three
      four five
     
      """.trimIndent(),
    )
    assertOffset(19)
  }

  // VIM-3731 |m| |]`|
  @Test
  fun testGotoNextMarkAfterLastMarkDoesNothing() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "jmb" + "wmc" + "ll"+ "]`"),
      """
      one two
      <caret>three
      four five
     
      """.trimIndent(),
    )
    assertOffset(21)
  }

  // |i| |`]|
  @Test
  fun testGotoLastChangePositionEnd() {
    doTest(
      Lists.newArrayList("yiw", "P", "gg", "`]"),
      """
     one two
     <caret>three
     four five
     
      """.trimIndent(),
      """
                one two
                thre<caret>ethree
                four five
                
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "This test freezes")
  @Test
  fun testVisualMarks() {
    configureByText("Oh, <caret>hi Mark")
    typeText(injector.parser.parseKeys("vw<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(4)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(7)
  }

  @Test
  fun testVisualMarksForBackwardsSelection() {
    configureByText("Oh, hi <caret>Mark")
    typeText(injector.parser.parseKeys("vb<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(4)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(7)
  }

  // we change start mark, but actually the start and end has changed
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "This test freezes")
  @Test
  fun testChangeSelectionStartMarkToBelowPosition() {
    configureByText("lala\nl<caret>alala\nlala\n")
    typeText(injector.parser.parseKeys("v3l<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("jhm<"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(14)
  }

  // we change end mark and only end changes
  @Test
  fun testChangeSelectionEndMarkToBelowPosition() {
    configureByText("lala\nl<caret>alala\nlala\n")
    typeText(injector.parser.parseKeys("v3l<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("jhm>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(14)
  }

  // we change start mark, but end changes
  @Test
  fun testChangeReversedSelectionStartMarkToBelowPosition() {
    configureByText("lala\nlala<caret>la\nlala\n")
    typeText(injector.parser.parseKeys("v3h<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("jhm<"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(14)
  }

  // we change end mark, but start and end are changed
  @Test
  fun testChangeReversedSelectionEndMarkToBelowPosition() {
    configureByText("lala\nlala<caret>la\nlala\n")
    typeText(injector.parser.parseKeys("v3h<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("jhm>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(14)
  }

  // we change start mark and only it changes
  @Test
  fun testChangeSelectionStartMarkToUpperPosition() {
    configureByText("lala\nl<caret>alala\nlala\n")
    typeText(injector.parser.parseKeys("v3l<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("khhm<"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(1)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
  }

  // we change end mark, but both start and end marks are changed
  @Test
  fun testChangeSelectionEndMarkToUpperPosition() {
    configureByText("lala\nl<caret>alala\nlala\n")
    typeText(injector.parser.parseKeys("v3l<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("khhm>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(1)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(6)
  }

  // we change end mark, but both start and end marks are changed
  @Test
  fun testChangeReversedSelectionStartMarkToUpperPosition() {
    configureByText("lala\nlala<caret>la\nlala\n")
    typeText(injector.parser.parseKeys("v3h<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("khhm<"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(1)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(6)
  }

  // we change end mark, but both start and end marks are changed
  @Test
  fun testChangeReversedSelectionEndMarkToUpperPosition() {
    configureByText("lala\nlala<caret>la\nlala\n")
    typeText(injector.parser.parseKeys("v3h<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(6)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
    typeText(injector.parser.parseKeys("khhm>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(1)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(9)
  }

  @Test
  fun testVisualLineSelectionMarks() {
    configureByText(
      """
    My mother taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("Vj<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(32)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(199)
  }

  @Test
  fun testReversedVisualLineSelectionMarks() {
    configureByText(
      """
    My mother taught me this trick:
    if you repeat something over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("Vk<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(32)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(199)
  }

  @Test
  fun testMulticaretMark() {
    configureByText(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("ma$")
    assertState(
      """
    My mother taught me this trick<caret>:
    if you repeat something over and over again it loses its meaning<caret>.
    For example: homework, homework, homework, homework, homework, homework, homework, homework, homework<caret>.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("`a")
    assertState(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
  }

  @Test
  fun testMulticaretSelectionMarks() {
    configureByText(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("vey\$p")
    assertState(
      """
    My mother taught me this trick:taught
    if you repeat something over and over again it loses its meaning.over
    For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.homework
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("gv")
    assertState(
      """
    My mother <selection>taugh<caret>t</selection> me this trick:taught
    if you repeat something <selection>ove<caret>r</selection> and over again it loses its meaning.over
    For example: homework, homework, homework, homework, <selection>homewor<caret>k</selection>, homework, homework, homework, homework.homework
    See, nothing.
    
      """.trimIndent(),
    )
  }

  @Test
  fun testMulticaretPreviousNextMark() {
    configureByText(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("mawmbw")
    assertState(
      """
    My mother taught me <caret>this trick:
    if you repeat something over and <caret>over again it loses its meaning.
    For example: homework, homework, homework, homework, homework, <caret>homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("[`")
    assertState(
      """
    My mother taught <caret>me this trick:
    if you repeat something over <caret>and over again it loses its meaning.
    For example: homework, homework, homework, homework, homework<caret>, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("[`")
    assertState(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("[`") // Does nothing on first mark.
    assertState(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
    typeText("5]`") // Excessive count goes to last mark.
    assertState(
      """
    My mother taught <caret>me this trick:
    if you repeat something over <caret>and over again it loses its meaning.
    For example: homework, homework, homework, homework, homework<caret>, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent(),
    )
  }
}

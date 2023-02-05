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
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.newapi.IjVimEditor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

@Suppress("SpellCheckingInspection")
class MarkTest : VimTestCase() {
  // |m|
  fun testLocalMark() {
    typeTextInFile(
      injector.parser.parseKeys("ma"),
      """    foo
    ba<caret>r
    baz
"""
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'a')
    assertNotNull(mark)
    assertEquals(1, mark!!.line)
    assertEquals(6, mark.col)
  }

  // |m|
  fun testGlobalMark() {
    typeTextInFile(
      injector.parser.parseKeys("mG"),
      """    foo
    ba<caret>r
    baz
"""
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'G')
    assertNotNull(mark)
    assertEquals(1, mark!!.line)
    assertEquals(6, mark.col)
  }

  // |m|
  fun testMarkIsDeletedWhenLineIsDeleted() {
    typeTextInFile(
      injector.parser.parseKeys("mx" + "dd"),
      """    foo
    ba<caret>r
    baz
"""
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'x')
    assertNull(mark)
  }

  // |m|
  fun testMarkIsNotDeletedWhenLineIsOneCharAndReplaced() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "r1"),
      """
     foo
     <caret>0
     bar
     
      """.trimIndent()
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'a')
    assertNotNull(mark)
  }

  // |m|
  fun testMarkIsNotDeletedWhenLineIsChanged() {
    typeTextInFile(
      injector.parser.parseKeys("ma" + "cc"),
      """    foo
    ba<caret>r
    baz
"""
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'a')
    assertNotNull(mark)
  }

  // |m|
  fun testMarkIsMovedUpWhenLinesArePartiallyDeletedAbove() {
    typeTextInFile(
      injector.parser.parseKeys("mx" + "2k" + "dd" + "0dw"),
      """    foo
    bar
    ba<caret>z
"""
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'x')
    assertNotNull(mark)
    assertEquals(1, mark!!.line)
    assertEquals(6, mark.col)
  }

  // |m|
  fun testMarkIsMovedUpWhenLinesAreDeletedAbove() {
    typeTextInFile(
      injector.parser.parseKeys("mx" + "2k" + "2dd"),
      """    foo
    bar
    ba<caret>z
"""
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'x')
    assertNotNull(mark)
    assertEquals(0, mark!!.line)
    assertEquals(6, mark.col)
  }

  // |m|
  fun testMarkIsMovedDownWhenLinesAreInsertedAbove() {
    typeTextInFile(
      injector.parser.parseKeys("mY" + "Obiff"),
      """
     foo
     ba<caret>r
     baz
     
      """.trimIndent()
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'Y')
    assertNotNull(mark)
    assertEquals(2, mark!!.line)
    assertEquals(2, mark.col)
  }

  // |m|
  fun testMarkIsMovedDownWhenLinesAreInsertedAboveWithIndentation() {
    typeTextInFile(
      injector.parser.parseKeys("mY" + "Obiff"),
      """    foo
    ba<caret>r
    baz
"""
    )
    val vimEditor: VimEditor = IjVimEditor(myFixture.editor)
    val mark = injector.markService.getMark(vimEditor.primaryCaret(), 'Y')
    assertNotNull(mark)
    assertEquals(2, mark!!.line)
    assertEquals(6, mark.col)
  }

  // |m| |`|
  fun testMarkAndJumpToMark() {
    typeTextInFile(
      injector.parser.parseKeys("6l" + "mZ" + "G$" + "`Z"),
      """    foo
    bar
    baz
"""
    )
    assertOffset(6)
  }

  // |m| |'|
  fun testMarkAndJumpToMarkLeadingSpace() {
    typeTextInFile(
      injector.parser.parseKeys("6l" + "mb" + "G$" + "'b"),
      """    foo
    bar
    baz
"""
    )
    assertOffset(4)
  }

  // |m| |`|
  fun testDeleteBacktickMotionIsCharacterWise() {
    typeTextInFile(
      injector.parser.parseKeys("mk" + "kh" + "d`k"),
      """    abcd
    efgh
    ij<caret>kl
    mnop
"""
    )
    assertState(
      """    abcd
    ekl
    mnop
"""
    )
  }

  // |m| |`|
  fun testDeleteSingleQuoteMotionIsLineWise() {
    typeTextInFile(
      injector.parser.parseKeys("mk" + "kh" + "d'k"),
      """    abcd
    efgh
    ij<caret>kl
    mnop
"""
    )
    assertState(
      """    abcd
    mnop
"""
    )
  }

  // VIM-43 |i| |`.|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  fun testGotoLastChangePosition() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "hello " + "<Esc>" + "gg" + "`."),
      """
                one two
                <caret>hello world
                three four
                
      """.trimIndent()
    )
    assertOffset(13)
  }

  // VIM-43 |p| |`.|
  fun testGotoLastPutPosition() {
    typeTextInFile(
      injector.parser.parseKeys("yy" + "p" + "gg" + "`."),
      """
     one two
     <caret>three
     four five
     
      """.trimIndent()
    )
    assertOffset(14)
  }

  // |i| |`]|
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun testVisualMarks() {
    configureByText("Oh, <caret>hi Mark")
    typeText(injector.parser.parseKeys("vw<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(4)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(7)
  }

  fun testVisualMarksForBackwardsSelection() {
    configureByText("Oh, hi <caret>Mark")
    typeText(injector.parser.parseKeys("vb<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(4)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(7)
  }

  // we change start mark, but actually the start and end has changed
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

  fun testVisualLineSelectionMarks() {
    configureByText(
      """
    My mother taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("Vj<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(32)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(199)
  }

  fun testReversedVisualLineSelectionMarks() {
    configureByText(
      """
    My mother taught me this trick:
    if you repeat something over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent()
    )
    typeText(injector.parser.parseKeys("Vk<Esc>"))
    typeText(injector.parser.parseKeys("`<"))
    assertOffset(32)
    typeText(injector.parser.parseKeys("`>"))
    assertOffset(199)
  }

  fun testMulticaretMark() {
    configureByText(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent()
    )
    typeText("ma$")
    assertState(
      """
    My mother taught me this trick<caret>:
    if you repeat something over and over again it loses its meaning<caret>.
    For example: homework, homework, homework, homework, homework, homework, homework, homework, homework<caret>.
    See, nothing.
    
      """.trimIndent()
    )
    typeText("`a")
    assertState(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent()
    )
  }

  fun testMulticaretSelectionMarks() {
    configureByText(
      """
    My mother <caret>taught me this trick:
    if you repeat something <caret>over and over again it loses its meaning.
    For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.
    See, nothing.
    
      """.trimIndent()
    )
    typeText("vey\$p")
    assertState(
      """
    My mother taught me this trick:taught
    if you repeat something over and over again it loses its meaning.over
    For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.homework
    See, nothing.
    
      """.trimIndent()
    )
    typeText("gv")
    assertState(
      """
    My mother <selection>taugh<caret>t</selection> me this trick:taught
    if you repeat something <selection>ove<caret>r</selection> and over again it loses its meaning.over
    For example: homework, homework, homework, homework, <selection>homewor<caret>k</selection>, homework, homework, homework, homework.homework
    See, nothing.
    
      """.trimIndent()
    )
  }
}

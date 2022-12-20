/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action;

import com.google.common.collect.Lists;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.VimStateMachine;
import com.maddyhome.idea.vim.mark.Mark;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;

/**
 * @author Tuomas Tynkkynen
 */
public class MarkTest extends VimTestCase {
  // |m|
  public void testLocalMark() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("ma"), "    foo\n" + "    ba<caret>r\n" + "    baz\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'a');
    assertNotNull(mark);
    assertEquals(1, mark.getLine());
    assertEquals(6, mark.getCol());
  }

  // |m|
  public void testGlobalMark() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("mG"), "    foo\n" + "    ba<caret>r\n" + "    baz\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'G');
    assertNotNull(mark);
    assertEquals(1, mark.getLine());
    assertEquals(6, mark.getCol());
  }

  // |m|
  public void testMarkIsDeletedWhenLineIsDeleted() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("mx" + "dd"), "    foo\n" + "    ba<caret>r\n" + "    baz\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'x');
    assertNull(mark);
  }

  // |m|
  public void testMarkIsNotDeletedWhenLineIsOneCharAndReplaced() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("ma" + "r1"), "foo\n" + "<caret>0\n" + "bar\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'a');
    assertNotNull(mark);
  }

  // |m|
  public void testMarkIsNotDeletedWhenLineIsChanged() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("ma" + "cc"), "    foo\n" + "    ba<caret>r\n" + "    baz\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'a');
    assertNotNull(mark);
  }

  // |m|
  public void testMarkIsMovedUpWhenLinesArePartiallyDeletedAbove() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("mx" + "2k" + "dd" + "0dw"), "    foo\n" + "    bar\n" + "    ba<caret>z\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'x');
    assertNotNull(mark);
    assertEquals(1, mark.getLine());
    assertEquals(6, mark.getCol());
  }

  // |m|
  public void testMarkIsMovedUpWhenLinesAreDeletedAbove() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("mx" + "2k" + "2dd"), "    foo\n" + "    bar\n" + "    ba<caret>z\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'x');
    assertNotNull(mark);
    assertEquals(0, mark.getLine());
    assertEquals(6, mark.getCol());
  }

  // |m|
  public void testMarkIsMovedDownWhenLinesAreInsertedAbove() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("mY" + "Obiff"), "foo\n" + "ba<caret>r\n" + "baz\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'Y');
    assertNotNull(mark);
    assertEquals(2, mark.getLine());
    assertEquals(2, mark.getCol());
  }

  // |m|
  public void testMarkIsMovedDownWhenLinesAreInsertedAboveWithIndentation() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("mY" + "Obiff"), "    foo\n" + "    ba<caret>r\n" + "    baz\n");
    VimEditor vimEditor = new IjVimEditor(myFixture.getEditor());
    Mark mark = injector.getMarkService().getMark(vimEditor.primaryCaret(), 'Y');
    assertNotNull(mark);
    assertEquals(2, mark.getLine());
    assertEquals(6, mark.getCol());
  }

  // |m| |`|
  public void testMarkAndJumpToMark() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("6l" + "mZ" + "G$" + "`Z"), "    foo\n" + "    bar\n" + "    baz\n");
    assertOffset(6);
  }

  // |m| |'|
  public void testMarkAndJumpToMarkLeadingSpace() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("6l" + "mb" + "G$" + "'b"), "    foo\n" + "    bar\n" + "    baz\n");
    assertOffset(4);
  }

  // |m| |`|
  public void testDeleteBacktickMotionIsCharacterWise() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("mk" + "kh" + "d`k"), "    abcd\n" + "    efgh\n" + "    ij<caret>kl\n" + "    mnop\n");
    assertState("    abcd\n" + "    ekl\n" + "    mnop\n");
  }

  // |m| |`|
  public void testDeleteSingleQuoteMotionIsLineWise() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("mk" + "kh" + "d'k"), "    abcd\n" + "    efgh\n" + "    ij<caret>kl\n" + "    mnop\n");
    assertState("    abcd\n" + "    mnop\n");
  }

  // VIM-43 |i| |`.|
  @TestWithoutNeovim(reason = SkipNeovimReason.UNCLEAR)
  public void testGotoLastChangePosition() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("i" + "hello " + "<Esc>" + "gg" + "`."),
                   "one two\n" + "<caret>hello world\n" + "three four\n");
    assertOffset(13);
  }

  // VIM-43 |p| |`.|
  public void testGotoLastPutPosition() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("yy" + "p" + "gg" + "`."), "one two\n" + "<caret>three\n" + "four five\n");
    assertOffset(14);
  }

  // |i| |`]|
  public void testGotoLastChangePositionEnd() {
    doTest(Lists.newArrayList("yiw", "P", "gg", "`]"), "one two\n" + "<caret>three\n" + "four five\n",
           "one two\n" + "thre<caret>ethree\n" + "four five\n", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testVisualMarks() {
    configureByText("Oh, <caret>hi Mark");
    typeText(injector.getParser().parseKeys("vw<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(4);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(7);
  }


  public void testVisualMarksForBackwardsSelection() {
    configureByText("Oh, hi <caret>Mark");
    typeText(injector.getParser().parseKeys("vb<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(4);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(7);
  }

  // we change start mark, but actually the start and end has changed
  public void testChangeSelectionStartMarkToBelowPosition() {
    configureByText("lala\nl<caret>alala\nlala\n");
    typeText(injector.getParser().parseKeys("v3l<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);

    typeText(injector.getParser().parseKeys("jhm<"));
    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(9);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(14);
  }

  // we change end mark and only end changes
  public void testChangeSelectionEndMarkToBelowPosition() {
    configureByText("lala\nl<caret>alala\nlala\n");
    typeText(injector.getParser().parseKeys("v3l<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);

    typeText(injector.getParser().parseKeys("jhm>"));
    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(14);
  }

  // we change start mark, but end changes
  public void testChangeReversedSelectionStartMarkToBelowPosition() {
    configureByText("lala\nlala<caret>la\nlala\n");
    typeText(injector.getParser().parseKeys("v3h<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);

    typeText(injector.getParser().parseKeys("jhm<"));
    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(14);
  }

  // we change end mark, but start and end are changed
  public void testChangeReversedSelectionEndMarkToBelowPosition() {
    configureByText("lala\nlala<caret>la\nlala\n");
    typeText(injector.getParser().parseKeys("v3h<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);

    typeText(injector.getParser().parseKeys("jhm>"));
    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(9);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(14);
  }

  // we change start mark and only it changes
  public void testChangeSelectionStartMarkToUpperPosition() {
    configureByText("lala\nl<caret>alala\nlala\n");
    typeText(injector.getParser().parseKeys("v3l<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);

    typeText(injector.getParser().parseKeys("khhm<"));
    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(1);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);
  }

  // we change end mark, but both start and end marks are changed
  public void testChangeSelectionEndMarkToUpperPosition() {
    configureByText("lala\nl<caret>alala\nlala\n");
    typeText(injector.getParser().parseKeys("v3l<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);

    typeText(injector.getParser().parseKeys("khhm>"));
    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(1);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(6);
  }

  // we change end mark, but both start and end marks are changed
  public void testChangeReversedSelectionStartMarkToUpperPosition() {
    configureByText("lala\nlala<caret>la\nlala\n");
    typeText(injector.getParser().parseKeys("v3h<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);

    typeText(injector.getParser().parseKeys("khhm<"));
    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(1);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(6);
  }

  // we change end mark, but both start and end marks are changed
  public void testChangeReversedSelectionEndMarkToUpperPosition() {
    configureByText("lala\nlala<caret>la\nlala\n");
    typeText(injector.getParser().parseKeys("v3h<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(6);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);

    typeText(injector.getParser().parseKeys("khhm>"));
    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(1);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(9);
  }

  public void testVisualLineSelectionMarks() {
    configureByText("My mother taught me this trick:\n" +
                    "if you repeat something <caret>over and over again it loses its meaning.\n" +
                    "For example: homework, homework, homework, homework, homework, homework, homework, homework, homework.\n" +
                    "See, nothing.\n");
    typeText(injector.getParser().parseKeys("Vj<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(32);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(199);
  }

  public void testReversedVisualLineSelectionMarks() {
    configureByText("My mother taught me this trick:\n" +
                    "if you repeat something over and over again it loses its meaning.\n" +
                    "For example: homework, homework, homework, homework, <caret>homework, homework, homework, homework, homework.\n" +
                    "See, nothing.\n");
    typeText(injector.getParser().parseKeys("Vk<Esc>"));

    typeText(injector.getParser().parseKeys("`<"));
    assertOffset(32);
    typeText(injector.getParser().parseKeys("`>"));
    assertOffset(199);
  }
}

/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action;

import com.intellij.idea.TestFor;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author Vasily Alferov
 */
public class MultipleCaretsTest extends VimTestCase {
  // com.maddyhome.idea.vim.action.visual.leftright

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLeftAction() {
    typeTextInFile(parseKeys("3h"), "abc<caret>de<caret>");
    assertState("<caret>ab<caret>cde");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionRightAction() {
    typeTextInFile(parseKeys("l"), "ab<caret>cd<caret>ef");
    assertState("abc<caret>de<caret>f");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMovementMerging() {
    Editor editor = typeTextInFile(parseKeys("2h"), "o<caret>n<caret>e");
    assertEquals(1, editor.getCaretModel().getCaretCount());
    assertState("<caret>one");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionColumnAction() {
    typeTextInFile(parseKeys("4|"), "one<caret> two\n" +
                                    "three four fiv<caret>e\n" +
                                    "si<caret>x seven<caret>\n" +
                                    "<caret>eig<caret>ht nine ten<caret>");
    assertState("one<caret> two\n" + "thr<caret>ee four five\n" + "six<caret> seven\n" + "eig<caret>ht nine ten");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionFirstColumnAction() {
    typeTextInFile(parseKeys("0"), "one<caret> two\n" +
                                   "three four fiv<caret>e\n" +
                                   "si<caret>x seven<caret>\n" +
                                   "<caret>eig<caret>ht nine te<caret>n");
    assertState("<caret>one two\n" + "<caret>three four five\n" + "<caret>six seven\n" + "<caret>eight nine ten");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionFirstNonSpaceAction() {
    typeTextInFile(parseKeys("^"),
                   "     one<caret> two\n" + "three<caret> four\n" + "  five<caret> six\n" + " <caret>  seven eight");
    assertState("     <caret>one two\n" + "<caret>three four\n" + "  <caret>five six\n" + "   <caret>seven eight");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLastNonSpaceAction() {
    typeTextInFile(parseKeys("g_"), "one<caret> two   \n" +
                                    "three<caret> four      \n" +
                                    " five si<caret>x\n" +
                                    "seven eight    <caret>  \n");
    assertState(
      "one tw<caret>o   \n" + "three fou<caret>r      \n" + " five si<caret>x\n" + "seven eigh<caret>t      \n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLastColumnAction() {
    typeTextInFile(parseKeys("$"), "one <caret>two\n" + "three fou<caret>r");
    assertState("one tw<caret>o\n" + "three fou<caret>r");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLeftMatchCharAction() {
    typeTextInFile(parseKeys("2Fa"), "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    assertState("<caret>a<caret>a<caret>ab<caret>ab<caret>ababbx");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionRightMatchCharAction() {
    typeTextInFile(parseKeys("2fb"), "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    assertState("aaaba<caret>baba<caret>b<caret>b<caret>x");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLeftTillMatchCharAction() {
    typeTextInFile(parseKeys("2Ta"), "b<caret>a<caret>ba<caret>a<caret>a<caret>ba<caret>b");
    assertState("b<caret>a<caret>ba<caret>a<caret>a<caret>bab");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionRightTillMatchCharAction() {
    typeTextInFile(parseKeys("2ta"), "<caret>b<caret>a<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
    assertState("ba<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLastLeftMatchChar() {
    typeTextInFile(parseKeys("Fa;"), "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    assertState("<caret>aa<caret>ab<caret>ab<caret>ababbx");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLastRightMatchChar() {
    typeTextInFile(parseKeys("fb;"), "<caret>a<caret>aabab<caret>ab<caret>ab<caret>b<caret>x");
    assertState("aaaba<caret>baba<caret>b<caret>b<caret>x");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLastRightTillMatchChar() {
    typeTextInFile(parseKeys("ta;"), "<caret>b<caret>a<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
    assertState("ba<caret>b<caret>a<caret>aa<caret>ba<caret>b");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLastMatchCharReverse() {
    typeTextInFile(parseKeys("fa", "2;", "3,"), "abaab<caret>a<caret>baaa<caret>abaaba");
    assertState("abaab<caret>abaaa<caret>abaaba");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionLeftWrap() {
    typeTextInFile(parseKeys("5<BS>"), "one\n" + "t<caret>wo three\n" + "fo<caret>ur\n");
    assertState("<caret>one\ntwo thr<caret>ee\nfour\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionRightWrap() {
    typeTextInFile(parseKeys("5<Space>"), "<caret>one\n" + "two thr<caret>ee\n" + "four\n");
    assertState("one\nt<caret>wo three\nfo<caret>ur\n");
  }

  // com.maddyhome.idea.vim.action.visual.updown

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionUpAction() {
    typeTextInFile(parseKeys("k"), "o<caret>ne\n" + "t<caret>wo<caret> \n" + "t<caret>hree<caret> ");
    assertState("o<caret>n<caret>e\n" + "t<caret>wo<caret> \n" + "three ");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionDownAction() {
    typeTextInFile(parseKeys("2j"),
                   "o<caret>n<caret>e\n" + "<caret>tw<caret>o          <caret> \n" + "three\n" + "four");
    assertState("one\n" + "two           \n" + "t<caret>h<caret>ree\n" + "<caret>fo<caret>u<caret>r");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testLeftRightAndUpDownMovements() {
    typeTextInFile(parseKeys("khj"), "abcde\n" + "ab<caret>cde\n" + "abc<caret>de\n" + "abcd<caret>e\n");
    assertState("abcde\n" + "a<caret>bcde\n" + "ab<caret>cde\n" + "abc<caret>de\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionDownFirstNonSpaceAction() {
    typeTextInFile(parseKeys("+"),
                   " <caret> on<caret>e<caret> two\n" + "<caret>   three<caret> four\n" + " five six\n");
    assertState("  one two\n" + "   <caret>three four\n" + " <caret>five six\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionDownLess1FirstNonSpaceActionWithNoCount() {
    typeTextInFile(parseKeys("_"),
                   "     one<caret> two\n" + "three<caret> four\n" + "  five<caret> six\n" + " <caret>  seven eight");
    assertState("     <caret>one two\n" + "<caret>three four\n" + "  <caret>five six\n" + "   <caret>seven eight");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionDownLess1FirstNonSpaceActionWithCount() {
    typeTextInFile(parseKeys("3_"), "x<caret>y<caret>z\n" +
                                    "  skip this <caret>line\n" +
                                    "   don't skip this line\n" +
                                    "    stop there\n");
    assertState("xyz\n" + "  skip this line\n" + "   <caret>don't skip this line\n" + "    <caret>stop there\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionUpFirstNonSpaceAction() {
    typeTextInFile(parseKeys("-"), " one\n" + "<caret>  tw<caret>o\n");
    assertState(" <caret>one\n" + "  two\n");
  }

  // com.maddyhome.idea.vim.action.visual.object

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBigWordAction() {
    typeTextInFile(parseKeys("v", "iW"), "a,<caret>bc<caret>d,e f,g<caret>hi,j");
    assertState("<selection>a,bcd,e</selection> <selection>f,ghi,j</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerWordAction() {
    typeTextInFile(parseKeys("v", "iw"), "a,<caret>bc<caret>d,e f,g<caret>hi,j");
    assertState("a,<selection>bcd</selection>,e f,<selection>ghi</selection>,j");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBlockAngleAction() {
    typeTextInFile(parseKeys("v", "2i<"), "<asdf<asdf<a<caret>sdf>a<caret>sdf>asdf> <asdf<as<caret>df>asdf>");
    assertState("<<selection>asdf<asdf<asdf>asdf>asdf</selection>> <<selection>asdf<asdf>asdf</selection>>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBlockBackQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i`"), "`as<caret>d<caret>f`asdf `a<caret>sdf`a<caret>sdf`a<caret>sdf`");
    assertState(
      "`<selection>asdf</selection>`asdf `<selection>asdf</selection>`<selection>asdf</selection>`<selection>asdf</selection>`");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBlockBraceAction() {
    typeTextInFile(parseKeys("v", "2i{"), "{asdf{asdf{a<caret>sdf}a<caret>sdf}asdf} {asdf{as<caret>df}asdf}");
    assertState("{<selection>asdf{asdf{asdf}asdf}asdf</selection>} {<selection>asdf{asdf}asdf</selection>}");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBlockBracketAction() {
    typeTextInFile(parseKeys("v", "2i["), "[asdf[asdf[a<caret>sdf]a<caret>sdf]asdf] [asdf[as<caret>df]asdf]");
    assertState("[<selection>asdf[asdf[asdf]asdf]asdf</selection>] [<selection>asdf[asdf]asdf</selection>]");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBlockDoubleQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i\""), "\"as<caret>d<caret>f\"asdf \"a<caret>sdf\"a<caret>sdf\"a<caret>sdf\"");
    assertState(
      "\"<selection>asdf</selection>\"asdf \"<selection>asdf</selection>\"<selection>asdf</selection>\"<selection>asdf</selection>\"");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBlockParenAction() {
    typeTextInFile(parseKeys("v", "2i("), "(asdf(asdf(a<caret>sdf)a<caret>sdf)asdf) (asdf(as<caret>df)asdf)");
    assertState("(<selection>asdf(asdf(asdf)asdf)asdf</selection>) (<selection>asdf(asdf)asdf</selection>)");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBlockSingleQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i'"), "'as<caret>d<caret>f'asdf 'a<caret>sdf'a<caret>sdf'a<caret>sdf'");
    assertState(
      "'<selection>asdf</selection>'asdf '<selection>asdf</selection>'<selection>asdf</selection>'<selection>asdf</selection>'");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerBlockTagAction() {
    typeTextInFile(parseKeys("v", "it"),
                   "<asdf1>qwer<asdf2>qwer<asdf3>qw<caret>er</asdf3>qw<caret>er</asdf2>qwer</asdf1>\n" +
                   "<asdf1>qwer<asdf2>qw<caret>er</asdf2>qwer</asdf1>");
    assertState("<asdf1>qwer<asdf2><selection>qwer<asdf3>qwer</asdf3>qwer</selection></asdf2>qwer</asdf1>\n" +
                "<asdf1>qwer<asdf2><selection>qwer</selection></asdf2>qwer</asdf1>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerParagraphAction() {
    typeTextInFile(parseKeys("v", "3ip"),
                   "a<caret>bcd\na<caret>bcd\n\nabcd\nabcd\n\na<caret>bcd\nabcd\n\nabcd\nabcd\n");
    assertState("<selection>abcd\nabcd\n\nabcd\nabcd\n</selection>\n<selection>abcd\nabcd\n\nabcd\nabcd\n</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionInnerSentenceAction() {
    typeTextInFile(parseKeys("v", "3is"), "a<caret>bcd a<caret>bcd. abcd abcd. a<caret>bcd abcd.");
    assertState("<selection>abcd abcd. abcd abcd.</selection><selection> abcd abcd.</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBigWordAction() {
    typeTextInFile(parseKeys("v", "aW"), " a<caret>bcd<caret>e.abcde.a<caret>bcde  a<caret>bcde.abcde\n");
    assertState(" <selection>abcde.abcde.abcde  </selection><selection>abcde.abcde</selection>\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterWordAction() {
    typeTextInFile(parseKeys("v", "aw"), " a<caret>bcd<caret>e.abcde.a<caret>bcde  a<caret>bcde.abcde");
    assertState(" <selection>abcde</selection>.abcde.<selection>abcde  abcde</selection>.abcde");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBlockAngleAction() {
    typeTextInFile(parseKeys("v", "2a<"), "<asdf<asdf<a<caret>sdf>a<caret>sdf>asdf> <asdf<a<caret>sdf>asdf>");
    assertState("<selection><asdf<asdf<asdf>asdf>asdf></selection> <selection><asdf<asdf>asdf></selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBlockBackQuoteAction() {
    typeTextInFile(parseKeys("v", "a`"), "`asdf`asdf`a<caret>sdf`a<caret>sdf`asdf` `asdf`a<caret>sdf`asdf`");
    assertState("`asdf`asdf<selection>`asdf`asdf`</selection>asdf` `asdf<selection>`asdf`</selection>asdf`");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBraceAction() {
    typeTextInFile(parseKeys("v", "2a{"), "{asdf{asdf{a<caret>sdf}a<caret>sdf}asdf} {asdf{a<caret>sdf}asdf}");
    assertState("<selection>{asdf{asdf{asdf}asdf}asdf}</selection> <selection>{asdf{asdf}asdf}</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBlockBracketAction() {
    typeTextInFile(parseKeys("v", "2a["), "[asdf[asdf[a<caret>sdf]a<caret>sdf]asdf] [asdf[a<caret>sdf]asdf]");
    assertState("<selection>[asdf[asdf[asdf]asdf]asdf]</selection> <selection>[asdf[asdf]asdf]</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBlockDoubleQuoteAction() {
    typeTextInFile(parseKeys("v", "a\""), "\"asdf\"asdf\"a<caret>sdf\"a<caret>sdf\"asdf\" \"asdf\"a<caret>sdf\"asdf\"");
    assertState("\"asdf\"asdf<selection>\"asdf\"asdf\"</selection>asdf\" \"asdf<selection>\"asdf\"</selection>asdf\"");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBlockParenAction() {
    typeTextInFile(parseKeys("v", "2a("), "(asdf(asdf(a<caret>sdf)a<caret>sdf)asdf) (asdf(a<caret>sdf)asdf)");
    assertState("<selection>(asdf(asdf(asdf)asdf)asdf)</selection> <selection>(asdf(asdf)asdf)</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBlockSingleQuoteAction() {
    typeTextInFile(parseKeys("v", "a'"), "'asdf'asdf'a<caret>sdf'a<caret>sdf'asdf' 'asdf'a<caret>sdf'asdf'");
    assertState("'asdf'asdf<selection>'asdf'asdf'</selection>asdf' 'asdf<selection>'asdf'</selection>asdf'");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterBlockTagAction() {
    typeTextInFile(parseKeys("v", "at"),
                   "<asdf1>qwer<asdf2>qwer<asdf3>qw<caret>er</asdf3>qw<caret>er</asdf2>qwer</asdf1>\n" +
                   "<asdf1>qwer<asdf2>qw<caret>er</asdf2>qwer</asdf1>");
    assertState("<asdf1>qwer<selection><asdf2>qwer<asdf3>qwer</asdf3>qwer</asdf2></selection>qwer</asdf1>\n" +
                "<asdf1>qwer<selection><asdf2>qwer</asdf2></selection>qwer</asdf1>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterParagraphAction() {
    typeTextInFile(parseKeys("v", "2ap"), "a<caret>sdf\n\na<caret>sdf\n\nasdf\n\n");
    assertState("<selection>asdf\n\nasdf\n\nasdf\n\n</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionOuterSentenceAction() {
    typeTextInFile(parseKeys("v", "2as"), "a<caret>sdf. a<caret>sdf. asdf.");
    assertState("<selection>asdf. asdf. asdf.</selection>");
  }

  // com.maddyhime.idea.vim.action.visual.text

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionBigWordEndLeftAction() {
    typeTextInFile(parseKeys("gE"), "a.asdf. a<caret>sdf<caret>.a a; as<caret>df\n a<caret>sdf");
    assertState("a.asdf<caret>. asdf.a a<caret>; asd<caret>f\n asdf");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionBigWordEndRightAction() {
    typeTextInFile(parseKeys("E"), "a<caret>.as<caret>df. a<caret>s<caret>df.a <caret> a; as<caret>df");
    assertState("a.asdf<caret>. asdf.<caret>a  a<caret>; asd<caret>f");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionBigWordLeftAction() {
    typeTextInFile(parseKeys("B"), "a<caret>.as<caret>df. a<caret>sdf.a <caret> a; as<caret>df");
    assertState("<caret>a.asdf. <caret>asdf.a  a; <caret>asdf");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionBigWordRightAction() {
    typeTextInFile(parseKeys("W"), "a<caret>.as<caret>df. a<caret>sdf.a <caret> a; as<caret>df");
    assertState("a.asdf. <caret>asdf.a  <caret>a; asd<caret>f");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionWordEndLeftAction() {
    typeTextInFile(parseKeys("ge"), "a.asdf. a<caret>sdf<caret>.a a; as<caret>df\n a<caret>sdf");
    assertState("a.asdf<caret>. asd<caret>f.a a<caret>; asd<caret>f\n asdf");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionWordEndRightAction() {
    typeTextInFile(parseKeys("e"), "a<caret>.as<caret>df. a<caret>s<caret>df.a <caret> a; as<caret>df");
    assertState("a.asd<caret>f. asd<caret>f.a  <caret>a; asd<caret>f");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionWordLeftAction() {
    typeTextInFile(parseKeys("b"), "a<caret>.as<caret>df. a<caret>sdf.a <caret> a; as<caret>df");
    assertState("<caret>a.<caret>asdf. <caret>asdf.<caret>a  a; <caret>asdf");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionWordRightAction() {
    typeTextInFile(parseKeys("w"), "a<caret>.as<caret>df. a<caret>sdf.a <caret> a; as<caret>df");
    assertState("a.<caret>asdf<caret>. asdf<caret>.a  <caret>a; asd<caret>f");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionCamelEndLeftAction() {
    typeTextInFile(parseKeys("2]b"),
                   "ClassName.M<caret>ethodN<caret>ame(<caret>Arg1Type arg1Na<caret>me, Arg2Type arg2<caret>Name) <caret>{");
    assertState(
      "Clas<caret>sNam<caret>e.Metho<caret>dName(Arg1Type ar<caret>g1Name, Arg2Type ar<caret>g<caret>2Name) {");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionCamelEndRightAction() {
    typeTextInFile(parseKeys("]w"),
                   "Cl<caret>assName.M<caret>ethodN<caret>ame(<caret>Arg1Type arg1Na<caret>me, Arg2Type ar<caret>g2<caret>Name) {");
    assertState(
      "Clas<caret>sName.Metho<caret>dNam<caret>e(Ar<caret>g1Type arg1Nam<caret>e, Arg2Type arg<caret>2Nam<caret>e) {");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionCamelLeftAction() {
    typeTextInFile(parseKeys("2[b"),
                   "ClassName.M<caret>ethodN<caret>ame(<caret>Arg1Type arg1Na<caret>me, Arg2Type arg2<caret>Name) <caret>{");
    assertState("Class<caret>Name.<caret>MethodName(Arg1Type arg<caret>1Name, Arg2Type <caret>arg<caret>2Name) {");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionCamelRightAction() {
    typeTextInFile(parseKeys("[w"),
                   "Cl<caret>assName.M<caret>ethodN<caret>ame(<caret>Arg1Type arg1Na<caret>me, Arg2Type ar<caret>g2Name) {");
    assertState(
      "Class<caret>Name.Method<caret>Name(<caret>Arg<caret>1Type arg1Name, <caret>Arg2Type arg<caret>2Name) {");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionMethodNextEndAction() {
    configureByJavaText("public class Foo {\n" +
                        "    private static void firstMethod(int argument) {\n" +
                        "        // Do som<caret>ething...\n" +
                        "    }\n" +
                        "    <caret>private static int x<caret>;" +
                        "    private static void secondMethod(String argument) {\n" +
                        "        // Do something.<caret>..\n" +
                        "    }\n" +
                        "}");
    typeText(parseKeys("]M"));
    assertState("public class Foo {\n" +
                "    private static void firstMethod(int argument) {\n" +
                "        // Do something...\n" +
                "    <caret>}\n" +
                "    private static int x<caret>;" +
                "    private static void secondMethod(String argument) {\n" +
                "        // Do something...\n" +
                "    <caret>}\n" +
                "}");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionMethodNextStartAction() {
    configureByJavaText("public class Foo {\n" +
                        " <caret>   private static void firstMethod(int argument) {\n" +
                        "        // Do som<caret>ething...\n" +
                        "    }\n" +
                        "    <caret>private static int x<caret>;" +
                        "    private static void secondMethod(String argument) {\n" +
                        "        // Do something.<caret>..\n" +
                        "    }\n" +
                        "}");
    typeText(parseKeys("]m"));
    assertState("public class Foo {\n" +
                "    private static void firstMethod(int argument) <caret>{\n" +
                "        // Do something...\n" +
                "    }\n" +
                "    <caret>private static int x;" +
                "    private static void secondMethod(String argument) <caret>{\n" +
                "        // Do something...\n" +
                "    }\n" +
                "}");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionMethodPreviousEndAction() {
    configureByJavaText("public class Foo {\n" +
                        " <caret>   private static void firstMethod(int argument) {\n" +
                        "        // Do som<caret>ething...\n" +
                        "    }\n" +
                        "    <caret>private static int x<caret>;" +
                        "    private static void secondMethod(String argument) {\n" +
                        "        // Do something.<caret>..\n" +
                        "    }\n" +
                        "}");
    typeText(parseKeys("[M"));
    assertState("public class Foo {\n" +
                "    private static void firstMethod(int argument) {\n" +
                "        // Do something...\n" +
                "    <caret>}\n" +
                "    private static int x<caret>;" +
                "    private static void secondMethod(String argument) {\n" +
                "        // Do something...\n" +
                "    }\n" +
                "}");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionMethodPreviousStartAction() {
    configureByJavaText("public class Foo {\n" +
                        " <caret>   private static void firstMethod(int argument) {\n" +
                        "        // Do som<caret>ething...\n" +
                        "    }\n" +
                        "    <caret>private static int x<caret>;" +
                        "    private static void secondMethod(String argument) {\n" +
                        "        // Do something.<caret>..\n" +
                        "    }\n" +
                        "}");
    typeText(parseKeys("[m"));
    assertState("public class Foo <caret>{\n" +
                "    private static void firstMethod(int argument) <caret>{\n" +
                "        // Do something...\n" +
                "    }\n" +
                "    <caret>private static int x;" +
                "    private static void secondMethod(String argument) <caret>{\n" +
                "        // Do something...\n" +
                "    }\n" +
                "}");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionNthCharacterAction() {
    typeTextInFile(parseKeys("5", "go"),
                   "<caret>on<caret>e two thr<caret>ee four fiv<caret>e six seven eigh<caret>t ni<caret>ne ten");
    assertState("one <caret>two three four five six seven eight nine ten");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionParagraphNextAction() {
    typeTextInFile(parseKeys("2}"), "o<caret>ne\n\n<caret>two\n\nthree\nthree\n\nfour\n\nfive");
    assertState("one\n\ntwo\n<caret>\nthree\nthree\n<caret>\nfour\n\nfive");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionParagraphPreviousAction() {
    typeTextInFile(parseKeys("2{"), "one\n\ntwo\n\nthree\nthree\n\nfou<caret>r\n\nfi<caret>ve");
    assertState("one\n\ntwo\n<caret>\nthree\nthree\n<caret>\nfour\n\nfive");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionSectionBackwardEndAction() {
    typeTextInFile(parseKeys("[]"), "no<caret>t_a_brace\n" +
                                    "{\n" +
                                    "<caret>not_a_brace\n" +
                                    "}\n" +
                                    "{\n" +
                                    "n<caret>ot_a_brace\n" +
                                    "}\n" +
                                    "not_a_<caret>brace");
    assertState("<caret>not_a_brace\n" +
                "{\n" +
                "not_a_brace\n" +
                "<caret>}\n" +
                "{\n" +
                "not_a_brace\n" +
                "<caret>}\n" +
                "not_a_brace");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionSectionBackwardStartAction() {
    typeTextInFile(parseKeys("[["), "n<caret>ot_a_brace\n" +
                                    "{\n" +
                                    "not_a_<caret>brace\n" +
                                    "<caret>}\n" +
                                    "{\n" +
                                    "not_a_b<caret>race\n" +
                                    "<caret>}\n" +
                                    "not_a_brace");
    assertState("<caret>not_a_brace\n" +
                "<caret>{\n" +
                "not_a_brace\n" +
                "}\n" +
                "<caret>{\n" +
                "not_a_brace\n" +
                "}\n" +
                "not_a_brace");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionSectionForwardEndAction() {
    typeTextInFile(parseKeys("]]"), "n<caret>ot_a_brace\n" +
                                    "{\n" +
                                    "n<caret>ot_a_brace\n" +
                                    "<caret>}\n" +
                                    "{\n" +
                                    "not_<caret>a_brace\n" +
                                    "}\n" +
                                    "not_a_brace");
    assertState("not_a_brace\n" +
                "{\n" +
                "not_a_brace\n" +
                "<caret>}\n" +
                "{\n" +
                "not_a_brace\n" +
                "<caret>}\n" +
                "not_a_brace");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionSectionForwardStartAction() {
    typeTextInFile(parseKeys("]["), "n<caret>ot_a_brace\n" +
                                    "{\n" +
                                    "n<caret>ot_a_brace\n" +
                                    "<caret>}\n" +
                                    "{\n" +
                                    "not_a_brace\n" +
                                    "}\n" +
                                    "not_a_brace");
    assertState("not_a_brace\n" +
                "<caret>{\n" +
                "not_a_brace\n" +
                "}\n" +
                "<caret>{\n" +
                "not_a_brace\n" +
                "}\n" +
                "not_a_brace");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionSentenceNextEndAction() {
    typeTextInFile(parseKeys("g)"), "a<caret>sdf<caret>. a<caret>sdf. a<caret>sdf.<caret> asdf.<caret> asdf.");
    assertState("asdf<caret>. asdf<caret>. asdf<caret>. asdf<caret>. asdf<caret>.");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionSentenceNextStartAction() {
    typeTextInFile(parseKeys(")"), "a<caret>sdf. <caret>asdf.<caret> asdf. <caret>asdf. asdf.");
    assertState("asdf. <caret>asdf. <caret>asdf. asdf. <caret>asdf.");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionSentencePreviousEndAction() {
    typeTextInFile(parseKeys("g("), "asdf.<caret> a<caret>sdf<caret>. as<caret>df. asd<caret>f. <caret>asdf.");
    assertState("asdf<caret>. asdf<caret>. asdf<caret>. asdf<caret>. asdf.");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionSentencePreviousStartAction() {
    typeTextInFile(parseKeys("("), "asd<caret>f. <caret>as<caret>df. asdf<caret>. asdf<caret>. as<caret>df.");
    assertState("<caret>asdf. <caret>asdf. <caret>asdf. <caret>asdf. <caret>asdf.");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionUnmatchedBraceCloseAction() {
    typeTextInFile(parseKeys("]}"), "{{}<caret> }<caret> }<caret> {}}<caret>{}}");
    assertState("{{} <caret>} <caret>} {}<caret>}{<caret>}}");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionUnmatchedBraceOpenAction() {
    typeTextInFile(parseKeys("[{"), "{<caret> {{}<caret> }{<caret>}{<caret>} ");
    assertState("<caret>{ <caret>{{} }<caret>{}<caret>{} ");
  }


  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionUnmatchedParenCloseAction() {
    typeTextInFile(parseKeys("])"), "(()<caret> )<caret> )<caret> ())<caret>())");
    assertState("(() <caret>) <caret>) ()<caret>)(<caret>))");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionUnmatchedParenOpenAction() {
    typeTextInFile(parseKeys("[("), "(<caret> (()<caret> )(<caret>)(<caret>) ");
    assertState("<caret>( <caret>(() )<caret>()<caret>() ");
  }

  // com.maddyhome.idea.vim.action.visual.visual

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualSwapEndsAction() {
    typeTextInFile(parseKeys("v", "iw", "o"), "o<caret>ne <caret>two th<caret>ree\n");
    assertState(
      "<selection><caret>one</selection> <selection><caret>two</selection> <selection><caret>three</selection>\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualToggleCharacterMode() {
    typeTextInFile(parseKeys("v", "e"), "o<caret>ne <caret>two th<caret>ree");
    assertState("o<selection>ne</selection> <selection>two</selection> th<selection>ree</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualToggleLineMode() {
    typeTextInFile(parseKeys("V", "2k"), "one two\n" +
                                         "three four\n" +
                                         "fi<caret>ve six\n" +
                                         "seven eight\n" +
                                         "nine ten\n" +
                                         "eleven twelve\n" +
                                         "th<caret>irteen fourteen\n");
    assertState("<selection>one two\n" +
                "three four\n" +
                "five six\n" +
                "</selection>seven eight\n" +
                "<selection>nine ten\n" +
                "eleven twelve\n" +
                "thirteen fourteen\n</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualModeMerging() {
    typeTextInFile(parseKeys("V", "j"), "one<caret> two\n" + "thr<caret>ee four\n" + "five six\n");
    assertState("<selection>one two\n" + "three four\n" + "five six\n</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualCharacterToVisualLineModeSwitch() {
    typeTextInFile(parseKeys("v", "k", "V"),
                   "one two\n" + "three fo<caret>ur\n" + "five six\n" + "seven eight\n" + "nine t<caret>en\n");
    assertState("<selection>one two\n" +
                "three four\n" +
                "</selection>five six\n" +
                "<selection>seven eight\n" +
                "nine ten\n</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualLineToVisualCharacterModeSwitch() {
    typeTextInFile(parseKeys("V", "k", "v"),
                   "one two\n" + "thre<caret>e four\n" + "five six\n" + "seven eight\n" + "n<caret>ine ten\n");
    assertState("one <selection>two\n" +
                "three</selection> four\n" +
                "five six\n" +
                "s<selection>even eight\n" +
                "ni</selection>ne ten\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualBlockDownAfterLineEndMovement() {
    typeTextInFile(parseKeys("<C-V>$j"), "abc\ndef\n");
    assertState("<selection>abc</selection>\n" + "<selection>def</selection>\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualBlockDownMovementAfterShorterLineAction() {
    typeTextInFile(parseKeys("<C-V>", "kkjj"), "one\n" + "\n" + "two three\n" + "four fi<caret>ve\n");
    assertState("one\n" + "\n" + "two three\n" + "four fi<selection><caret>v</selection>e\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualBlockDownMovementWithEmptyLineInMiddle() {
    typeTextInFile(parseKeys("<C-V>", "3k", "j"), "one\n" + "\n" + "two three\n" + "four fi<caret>ve\n");
    assertState("one\n" + "\n" + "<selection>two thre</selection>e\n" + "<selection>four fiv</selection>e\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualBlockDownMovementWithManyEmptyLinesInMiddle() {
    typeTextInFile(parseKeys("<C-V>", "4kjjj"), "one\n" + "\n" + "\n" + "two three\n" + "four fi<caret>ve\n");
    assertState("one\n" + "\n" + "\n" + "two thr<selection>e</selection>e\n" + "four fi<selection>v</selection>e\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMergingSelections() {
    typeTextInFile(parseKeys("v", "aW", "l", "h"), "a<caret>bcde.abcde.abcde  ab<caret>cde.abcde\n");
    assertState("<selection>abcde.abcde.abcde  abcde.abcde</selection>\n");

  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualMotionUp() {
    typeTextInFile(parseKeys("v", "k", "k"), "abcde\nabcde\nab<caret>cde\n");
    assertState("ab<selection>cde\nabcde\nabc</selection>de\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualMotionDown() {
    typeTextInFile(parseKeys("v", "2j", "j"), "ab<caret>cde\nabcde\n\nabcde\n");
    assertState("ab<selection>cde\nabcde\n\nabc</selection>de\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualLineMotionUp() {
    typeTextInFile(parseKeys("V", "2k", "k"), "abcde\nabcde\n\nab<caret>cde\nabcde\n");
    assertState("<selection>ab<caret>cde\nabcde\n\nabcde\n</selection>abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualLineMotionDown() {
    typeTextInFile(parseKeys("V", "2j", "j"), "ab<caret>cde\nabcde\n\nabcde\nabcde\n");
    assertState("<selection>abcde\nabcde\n\nab<caret>cde\n</selection>abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualCharacterUpMerging() {
    typeTextInFile(parseKeys("v", "2k", "k"), "abcde\nabcde\n\nabc<caret>de\nab<caret>cde\n");
    assertState("abc<selection><caret>de\nabcde\n\nabcde\nabc</selection>de\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualCharacterDownMerging() {
    typeTextInFile(parseKeys("v", "2j", "j"), "abc<caret>de\nab<caret>cde\n\nabcde\nabcde\n");
    assertState("abc<selection>de\nabcde\n\nabcde\nab<caret>c</selection>de\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualLineUpMerging() {
    typeTextInFile(parseKeys("V", "2k", "k"), "abcde\nabcde\n\nabc<caret>de\nab<caret>cde\n");
    assertState("<selection>abc<caret>de\nabcde\n\nabcde\nabcde\n</selection>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testVisualLineDownMerging() {
    typeTextInFile(parseKeys("V", "2j", "j"), "abc<caret>de\nab<caret>cde\n\nabcde\nabcde\n");
    assertState("<selection>abcde\nabcde\n\nabcde\nab<caret>cde\n</selection>");
  }

  // com.maddyhome.idea.vim.action.change.change

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testAutoIndentLinesVisualAction() {
    configureByJavaText("<caret>public class Foo {\n" +
                        "private boolean x;\n" +
                        "                         private boolean y;\n" +
                        "private boolean z;\n" +
                        "<caret>public void foo() {\n" +
                        "x = true; // This will be indented\n" +
                        "}\n" +
                        "public void bar() {\n" +
                        "y = true; // And this will not\n" +
                        "}\n" +
                        "}\n");
    typeText(parseKeys("V2j="));
    assertState("<caret>public class Foo {\n" +
                "    private boolean x;\n" +
                "    private boolean y;\n" +
                "private boolean z;\n" +
                "    <caret>public void foo() {\n" +
                "        x = true; // This will be indented\n" +
                "    }\n" +
                "public void bar() {\n" +
                "y = true; // And this will not\n" +
                "}\n" +
                "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCaseLowerMotionAction() {
    typeTextInFile(parseKeys("gu2w"), "O<caret>NcE thIs <caret>TEXt wIlL n<caret>Ot lOoK s<caret>O rIdIcuLoUs\n");
    assertState("O<caret>nce this <caret>text will n<caret>ot look s<caret>o ridiculous\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCaseLowerVisualAction() {
    typeTextInFile(parseKeys("v2wu"), "O<caret>NcE thIs <caret>TEXt wIlL n<caret>Ot lOoK s<caret>O rIdIcuLoUs\n");
    assertState("O<caret>nce this text will n<caret>ot look s<caret>o ridiculous\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCaseToggleCharacterAction() {
    typeTextInFile(parseKeys("5~"), "OnE t<caret>Wo <caret>ThReE<caret> fOuR fIvE\n");
    assertState("OnE twO Th<caret>rEe<caret> FoUr<caret> fIvE\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCaseToggleMotionAction() {
    typeTextInFile(parseKeys("g~e"), "<caret>capitalize <caret>UNCAPITALIZE<caret> <caret>sTaY\n");
    assertState("<caret>CAPITALIZE <caret>uncapitalize<caret> <caret>sTaY\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCaseToggleVisualAction() {
    typeTextInFile(parseKeys("ve~"), "<caret>capitalize <caret>UNCAPITALIZE\n");
    assertState("<caret>CAPITALIZE <caret>uncapitalize\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCaseUpperMotionAction() {
    typeTextInFile(parseKeys("gU2w"), "O<caret>NcE thIs <caret>TEXt wIlL <caret>nOt lOoK <caret>sO rIdIcuLoUs\n");
    assertState("O<caret>NCE THIS <caret>TEXT WILL <caret>NOT LOOK <caret>SO RIDICULOUS\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCaseUpperVisualAction() {
    typeTextInFile(parseKeys("v2wU"), "O<caret>NcE thIs <caret>TEXt wIlL N<caret>Ot lOoK S<caret>O rIdIcuLoUs\n");
    assertState("O<caret>NCE THIS TEXT WILL N<caret>OT LOOK S<caret>O RIDICULOUS\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCharacterAction() {
    typeTextInFile(parseKeys("rz"), "on<caret>e <caret>t<caret>w<caret>o th<caret>r<caret>ee");
    assertState("on<caret>z <caret>z<caret>z<caret>z th<caret>z<caret>ze");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCharacterActionWithCount() {
    typeTextInFile(parseKeys("2rz"), "on<caret>e <caret>t<caret>w<caret>o th<caret>r<caret>ee");
    assertState("on<caret>zz<caret>z<caret>z<caret>zzth<caret>z<caret>zz");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeCharactersAction() {
    typeTextInFile(parseKeys("4s", "<ESC>"), "on<caret>e two <caret>th<caret>ee four five\n");
    assertState("o<caret>no<caret> r five\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeEndOfLineAction() {
    typeTextInFile(parseKeys("Cabc", "<ESC>"),
                   "a<caret>bcde\n" + "abcde\n" + "a<caret>bcde\n" + "a<caret>bcd<caret>e\n" + "abcde\n");
    assertState("aab<caret>c\n" + "abcde\n" + "aab<caret>c\n" + "aab<caret>c\n" + "abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeLineAction() {
    typeTextInFile(parseKeys("c2ca", "<ESC>"), "ab<caret>cde\n" + "abcde\n" + "abcde\n" + "abc<caret>de\n" + "abcde\n");
    assertState("<caret>a\n" + "abcde\n" + "<caret>a\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testOneCaretPositionAfterChangeLineAction() {
    typeTextInFile(parseKeys("c2c", "<ESC>"), "abcde\n" + "ab<caret>cde\n" + "abcde\n" + "abcde\n");
    assertState("abcde\n" + "<caret>\n" + "abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testCaretPositionAfterChangeLineAction() {
    typeTextInFile(parseKeys("c2c", "<ESC>"), "abcd<caret>e\n" +
                                              "abcde\n" +
                                              "abcde\n" +
                                              "ab<caret>cde\n" +
                                              "abcde\n" +
                                              "abcde\n" +
                                              "<caret>abcde\n" +
                                              "abcde\n");
    assertState("<caret>\n" + "abcde\n" + "<caret>\n" + "abcde\n" + "<caret>\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeMotionAction() {
    typeTextInFile(parseKeys("ciw", "correct", "<ESC>"), "correct correct wron<caret>g wr<caret>ong correct\n");
    assertState("correct correct correc<caret>t correc<caret>t correct\n");

  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeNumberIncAction() {
    typeTextInFile(parseKeys("<C-A>"), "1<caret>7<caret>7 2<caret>38 <caret>999\n");
    assertState("17<caret>9 23<caret>9 100<caret>0\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeNumberDecAction() {
    typeTextInFile(parseKeys("<C-X>"), "1<caret>8<caret>1 2<caret>40 <caret>1001\n");
    assertState("17<caret>9 23<caret>9 100<caret>0\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeReplaceAction() {
    typeTextInFile(parseKeys("Rz", "<ESC>"), "on<caret>e <caret>t<caret>w<caret>o th<caret>r<caret>ee");
    assertState("on<caret>z <caret>z<caret>z<caret>z th<caret>z<caret>ze");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeReplaceActionWithSeveralCharacters() {
    final String before = "<caret>qwe\n" + "asd <caret>zxc\n" + "qwe<caret>asdzxc";
    typeTextInFile(parseKeys("Rrty", "<Esc>"), before);
    final String after = "rt<caret>y\n" + "asd rt<caret>y\n" + "qwert<caret>yzxc";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeVisualCharacterAction() {
    typeTextInFile(parseKeys("v2lra"), "abcd<caret>ffffff<caret>abcde<caret>aaaa\n");
    assertState("abcdaa<caret>afffaa<caret>adeaa<caret>aa\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeVisualLinesAction() {
    typeTextInFile(parseKeys("VjS", "abcde", "<ESC>"),
                   "gh<caret>ijk\n" + "ghijk\n" + "abcde\n" + "ghi<caret>jk\n" + "ghijk\n");
    assertState("abcd<caret>e\n" + "abcde\n" + "abcd<caret>e\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testChangeVisualLinesEndAction() {
    typeTextInFile(parseKeys("vjC", "abcde", "<ESC>"),
                   "gh<caret>ijk\n" + "ghijk\n" + "abcde\n" + "ghi<caret>jk\n" + "ghijk\n");
    assertState("abcd<caret>e\n" + "abcde\n" + "abcd<caret>e\n");
  }

  // com.maddyhome.idea.vim.action.change.delete

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteCharacterAction() {
    typeTextInFile(parseKeys("<Del>"), "a<caret>bcde\n" + "<caret>abcde\n" + "abcd<caret>e\n");
    assertState("a<caret>cde\n" + "<caret>bcde\n" + "abc<caret>d\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteCharacterActionOrder() {
    typeTextInFile(parseKeys("<Del>"), "ab<caret>c<caret>d<caret>e abcde\n");
    assertState("ab<caret> abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteCharacterLeftAction() {
    typeTextInFile(parseKeys("3X"), "a<caret>bcde\n" + "<caret>abcde\n" + "abcd<caret>e\n");
    assertState("<caret>bcde\n" + "<caret>abcde\n" + "a<caret>e\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteCharacterLeftCaretMerging() {
    typeTextInFile(parseKeys("3X"), "a<caret>bc<caret>def<caret>ghij<caret>klmn<caret>op<caret>q");
    assertState("gq");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteCharacterRightAction() {
    typeTextInFile(parseKeys("3x"), "a<caret>bcde\n" + "<caret>abcde\n" + "abcd<caret>e\n");
    assertState("a<caret>e\n" + "<caret>de\n" + "abc<caret>d\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteCharacterRightCaretMerging() {
    typeTextInFile(parseKeys("4x"), "o<caret>ne <caret>two <caret>three four");
    assertState("o<caret> four");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteEndOfLineAction() {
    typeTextInFile(parseKeys("D"), "abcd<caret>e\n" +
                                   "abcde\n" +
                                   "abc<caret>de\n" +
                                   "<caret>abcde\n" +
                                   "ab<caret>cde\n" +
                                   "ab<caret>cd<caret>e\n");
    assertState("abc<caret>d\n" + "abcde\n" + "ab<caret>c\n" + "<caret>\n" + "a<caret>b\n" + "a<caret>b\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteEndOfLineActionWithCount() {
    typeTextInFile(parseKeys("3D"), "ab<caret>cde\n" +
                                    "abcde\n" +
                                    "abcde\n" +
                                    "abcd<caret>e\n" +
                                    "a<caret>bcd<caret>e\n" +
                                    "abc<caret>de\n");
    assertState("ab\n" + "abcd");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteJoinLinesAction() {
    typeTextInFile(parseKeys("gJ"), "ab<caret>cde\n" +
                                    "abcde\n" +
                                    "ab<caret>cde\n" +
                                    "abcd<caret>e\n" +
                                    "abcde\n" +
                                    "abc<caret>de\n" +
                                    "  abcde\n");
    assertState("abcde<caret>abcde\n" + "abcde<caret>abcde<caret>abcde\n" + "abcde<caret>  abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteJoinLinesSimpleAction() {
    typeTextInFile(parseKeys("gJ"), "a<caret>bcde\n" + "abcde\n");
    assertState("abcde<caret>abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteJoinLinesSpacesAction() {
    typeTextInFile(parseKeys("J"), "ab<caret>cde\n" +
                                   "abcde\n" +
                                   "ab<caret>cde\n" +
                                   "abcd<caret>e\n" +
                                   "abcde\n" +
                                   "abc<caret>de\n" +
                                   "  abcde\n");
    assertState("abcde<caret> abcde\n" + "abcde<caret> abcde<caret> abcde\n" + "abcde<caret> abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteJoinVisualLinesAction() {
    typeTextInFile(parseKeys("VkgJ"), "one\n" + "tw<caret>o\n" + "three\n" + "fo<caret>ur\n");
    assertState("one<caret>two\n" + "three<caret>four\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteJoinVisualLinesSpacesAction() {
    typeTextInFile(parseKeys("VkJ"), "abcde\n" + "abcd<caret>e\n" + "abcde\n" + "ab<caret>cde\n");
    assertState("abcde<caret> abcde\n" + "abcde<caret> abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteVisualAction() {
    typeTextInFile(parseKeys("vlj"), "abc<caret>de\n" + "<caret>abcde\n" + "abc<caret>de\n" + "abcde\n");
    assertState(
      "abc<selection>de\n" + "abcde\n" + "a<caret>b</selection>c<selection>de\n" + "abcd<caret>e</selection>\n");
    typeText(parseKeys("d"));
    assertState("abc<caret>c\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteVisualActionWithMultipleCaretsLeft() {
    typeTextInFile(parseKeys("v", "fd", "d"), "a<caret>bcde\n" + "abcde\n" + "<caret>abcde\n" + "ab<caret>cde\n");
    assertState("a<caret>e\n" + "abcde\n" + "<caret>e\n" + "ab<caret>e\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testDeleteVisualLinesAction() {
    typeTextInFile(parseKeys("Vjd"), "abc<caret>de\n" + "abcde\n" + "abcde\n" + "a<caret>bcde\n" + "abcde\n");
    assertState("<caret>abcde\n<caret>");
  }

  // com.maddyhome.idea.vim.action.change.insert

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertEscape() {
    typeTextInFile(parseKeys("i", "<ESC>", "i", "<ESC>"), "on<caret>e tw<caret>o th<caret>ree");
    assertState("<caret>one <caret>two <caret>three");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertAfterCursorActionMovement() {
    typeTextInFile(parseKeys("a", "<ESC>"), "on<caret>e two th<caret>ree");
    assertState("on<caret>e two th<caret>ree");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertAfterCursorAction() {
    typeTextInFile(parseKeys("a", "abcd", "<ESC>"), "on<caret>e two th<caret>re<caret>e");
    assertState("oneabc<caret>d two thrabc<caret>deeabc<caret>d");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertBeforeCursorAction() {
    typeTextInFile(parseKeys("i", "four", "<ESC>"), "one two three <caret> \n" + "seven six five <caret> \n");
    assertState("one two three fou<caret>r \n" + "seven six five fou<caret>r \n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertBeforeFirstNonBlankAction() {
    typeTextInFile(parseKeys("I", "four ", "<ESC>"),
                   "  three t<caret>wo on<caret>e\n" + "<caret> five six se<caret>ven eight\n");
    assertState("  four<caret> three two one\n" + " four<caret> five six seven eight\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertCharacterAboveCursorAction() {
    typeTextInFile(parseKeys("a", "<C-Y>", "<C-Y>", "<C-Y>", "<ESC>"), " one two three four\n" +
                                                                       "<caret>  two three four\n" +
                                                                       "four three two one\n" +
                                                                       "four three two<caret> \n");
    assertState(" one two three four\n" +
                " on<caret>e two three four\n" +
                "four three two one\n" +
                "four three two on<caret>e\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertCharacterBelowCursorAction() {
    typeTextInFile(parseKeys("a", "<C-E>", "<C-E>", "<C-E>", "<ESC>"), "<caret>  two three four\n" +
                                                                       " one two three four\n" +
                                                                       "four three two<caret> \n" +
                                                                       "four three two one\n");
    assertState(" on<caret>e two three four\n" +
                " one two three four\n" +
                "four three two on<caret>e\n" +
                "four three two one\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertDeleteInsertedTextAction() {
    typeTextInFile(parseKeys("a", "asdf", "<C-U>", "<ESC>"), "on<caret>e two th<caret>ree");
    assertState("on<caret>e two th<caret>ree");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertEnterAction() {
    typeTextInFile(parseKeys("i", "<C-M>", "<ESC>"), "one<caret>two<caret>three<caret>four\n");
    assertState("one\n" + "<caret>two\n" + "<caret>three\n" + "<caret>four\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertLineStartAction() {
    typeTextInFile(parseKeys("gI", "four ", "<ESC>"),
                   "  three t<caret>wo on<caret>e\n" + "<caret> five six se<caret>ven eight\n");
    assertState("four<caret>   three two one\n" + "four<caret>  five six seven eight\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertNewLineAboveAction() {
    typeTextInFile(parseKeys("O", "abcde", "<ESC>"),
                   "ab<caret>cde\n" + "ab<caret>cde\n" + "abcde\n" + "abc<caret>de\n");
    assertState("abcd<caret>e\n" + "abcde\n" + "abcd<caret>e\n" + "abcde\n" + "abcde\n" + "abcd<caret>e\n" + "abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertNewLineAboveActionWithMultipleCaretsInLine() {
    typeTextInFile(parseKeys("O", "<ESC>"), "a<caret>bcd<caret>e\n" + "abc<caret>d<caret>e\n");
    assertState("<caret>\n" + "abcde\n" + "<caret>\n" + "abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertNewLineBelowAction() {
    typeTextInFile(parseKeys("o", "abcde", "<ESC>"),
                   "ab<caret>cde\n" + "ab<caret>cde\n" + "abcde\n" + "abc<caret>de\n");
    assertState("abcde\n" + "abcd<caret>e\n" + "abcde\n" + "abcd<caret>e\n" + "abcde\n" + "abcde\n" + "abcd<caret>e\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertSingleCommandAction() {
    typeTextInFile(parseKeys("i", "<C-O>", "2h", "<ESC>"), "one <caret>two <caret>three <caret>four\n");
    assertState("o<caret>ne t<caret>wo thr<caret>ee four\n");
  }

  // com.maddyhome.idea.vim.action.change.shift

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testShiftLeftLinesAction() {
    typeTextInFile(parseKeys("2<<"),
                   "        <caret>abcde\n" + "        abcde\n" + "    abcde\n" + "    <caret>abcde\n" + "    abcde\n");
    assertState("    <caret>abcde\n" + "    abcde\n" + "    abcde\n" + "<caret>abcde\n" + "abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testShiftLeftMotionAction() {
    typeTextInFile(parseKeys("<j"),
                   "        <caret>abcde\n" + "        abcde\n" + "    abcde\n" + "    <caret>abcde\n" + "    abcde\n");
    assertState("    <caret>abcde\n" + "    abcde\n" + "    abcde\n" + "<caret>abcde\n" + "abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testShiftLeftVisualAction() {
    typeTextInFile(parseKeys("Vj<"),
                   "        <caret>abcde\n" + "        abcde\n" + "    abcde\n" + "    <caret>abcde\n" + "    abcde\n");
    assertState("    <caret>abcde\n" + "    abcde\n" + "    abcde\n" + "<caret>abcde\n" + "abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testShiftRightLinesAction() {
    typeTextInFile(parseKeys("2>>"),
                   "    <caret>abcde\n" + "    abcde\n" + "    abcde\n" + "<caret>abcde\n" + "abcde\n");
    assertState("        <caret>abcde\n" + "        abcde\n" + "    abcde\n" + "    <caret>abcde\n" + "    abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testShiftRightMotionAction() {
    typeTextInFile(parseKeys(">j"),
                   "    <caret>abcde\n" + "    abcde\n" + "    abcde\n" + "<caret>abcde\n" + "abcde\n");
    assertState("        <caret>abcde\n" + "        abcde\n" + "    abcde\n" + "    <caret>abcde\n" + "    abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testShiftRightVisualAction() {
    typeTextInFile(parseKeys("Vj>"),
                   "    <caret>abcde\n" + "    abcde\n" + "    abcde\n" + "<caret>abcde\n" + "abcde\n");
    assertState("        <caret>abcde\n" + "        abcde\n" + "    abcde\n" + "    <caret>abcde\n" + "    abcde\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionGoToLineFirst() {
    typeTextInFile(parseKeys("i", "<C-Home>"), "    sdf" +
                                               "dsfa<caret>dsf fg dsfg sd<caret>fjgkfdgl jsdf" +
                                               "nflgj sd\n dflgj dfdsfg\n dfsgj sdf<caret>klgj");
    assertState("<caret>    sdf" + "dsfadsf fg dsfg sdfjgkfdgl jsdf" + "nflgj sd\n dflgj dfdsfg\n dfsgj sdfklgj");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionGotoLineLastEnd() {
    typeTextInFile(parseKeys("<C-End>"), "    sdfdsfa<caret>dsf fg dsfg sd<caret>fjgkfdgl jsdf\n" +
                                         "nflgj sd\n" +
                                         " dflgj dfdsfg\n" +
                                         " hdfsgj sdf<caret>klgj\n");
    assertState(
      "    sdfdsfadsf fg dsfg sdfjgkfdgl jsdf\n" + "nflgj sd\n" + " dflgj dfdsfg\n" + " hdfsgj sdfklgj\n<caret>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionGotoLineLastEndInsertMode() {
    typeTextInFile(parseKeys("i", "<C-End>"), "    sdfdsfa<caret>dsf fg dsfg sd<caret>fjgkfdgl jsdf\n" +
                                              "nflgj sd\n" +
                                              " dflgj dfdsfg\n" +
                                              " hdfsgj sdf<caret>klgj\n");
    assertState(
      "    sdfdsfadsf fg dsfg sdfjgkfdgl jsdf\n" + "nflgj sd\n" + " dflgj dfdsfg\n" + " hdfsgj sdfklgj\n<caret>");
  }

  @TestFor(classes = {SearchWholeWordForwardAction.class})
  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testSearchWholeWordForwardAction() {
    typeTextInFile(parseKeys("2*"), "q<caret>we as<caret>d zxc qwe asd zxc qwe asd zxc qwe asd zxc qwe asd zxc ");
    assertState("qwe asd zxc qwe asd zxc <caret>qwe <caret>asd zxc qwe asd zxc qwe asd zxc ");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testSearchWholeWordBackwardAction() {
    typeTextInFile(parseKeys("2#"), "qwe asd zxc qwe asd zxc <caret>qwe <caret>asd zxc qwe asd zxc qwe asd zxc ");
    assertState("<caret>qwe <caret>asd zxc qwe asd zxc qwe asd zxc qwe asd zxc qwe asd zxc ");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionPercentOrMatchAction() {
    typeTextInFile(parseKeys("%"),
                   "fdgkh<caret>sjh thsth[ sd<caret>k er{}gha re<caret>ghrjae (ghoefgh kjfgh)sdgfh dgfh]");
    assertState("fdgkhsjh thsth[ sdk er{<caret>}gha reghrjae (ghoefgh kjfgh<caret>)sdgfh dgfh<caret>]");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionGotoLineLastAction() {
    typeTextInFile(parseKeys("G"), "dfgdfsg<caret>gfdfgdfs dasgdfsk dfghsdfkj gh\n" +
                                   "lsdjf lsj<caret> flk gjdlsadlsfj <caret>lksdgfj \n" +
                                   "dflgjdfsgk<caret>d<caret>flgjdfsklg\n\n");
    assertState("dfgdfsggfdfgdfs dasgdfsk dfghsdfkj gh\n" +
                "lsdjf lsj flk gjdlsadlsfj lksdgfj \n" +
                "dflgjdfsgkdflgjdfsklg\n\n<caret>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testMotionGotoLineLastWithArgumentAction() {
    typeTextInFile(parseKeys("1G"), "dfgdfsg<caret>gfdfgdfs dasgdfsk dfghsdfkj gh\n" +
                                    "lsdjf lsj<caret> flk gjdlsadlsfj <caret>lksdgfj \n" +
                                    "dflgjdfsgk<caret>d<caret>flgjdfsklg\n\n");
    assertState("<caret>dfgdfsggfdfgdfs dasgdfsk dfghsdfkj gh\n" +
                "lsdjf lsj flk gjdlsadlsfj lksdgfj \n" +
                "dflgjdfsgkdflgjdfsklg\n\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testInsertAtPreviousInsert() {
    final String before = "qw<caret>e\n" + "  a<caret>s<caret>d\n" + "zx<caret>c";
    typeTextInFile(parseKeys("I", "rty", "<Esc>", "2lj", "gi", "fgh", "<Esc>"), before);
    final String after = "rtyqwe\n" + "  rtyasd\n" + "rtyfg<caret>hzxc";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testAutoIndentRange() {
    final String before = "cl<caret>ass C {\n C(int i) {\nmy<caret>I = i;\n}\n private int myI;\n}";
    configureByJavaText(before);
    typeText(parseKeys("v2j="));
    final String after =
      "<caret>class C {\n" + "    C(int i) {\n" + "        myI = i;\n" + "    }\n" + "    private int myI;\n" + "}";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testAutoIndentMotion() {
    final String before = "cl<caret>ass C {\n C(int i) {\nmy<caret>I = i;\n}\n private int myI;\n}";
    configureByJavaText(before);
    typeText(parseKeys("=3j"));
    final String after = "<caret>class C {\n" +
                         "    C(int i) {\n" +
                         "        <caret>myI = i;\n" +
                         "    }\n" +
                         "    private int myI;\n" +
                         "}";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testAutoIndentLines() {
    final String before = "class C {\n C<caret>(int i) {\nmyI = i;\n}\n p<caret>rivate int myI;\n}";
    configureByJavaText(before);
    typeText(parseKeys("=="));
    final String after = "class C {\n    <caret>C(int i) {\nmyI = i;\n}\n    <caret>private int myI;\n}";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextBeforeCursor() {
    final String before = "<caret>qwe asd <caret>zxc rty <caret>fgh vbn";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(16, 19), SelectionType.CHARACTER_WISE, false);
    typeText(parseKeys("P", "3l", "P"));
    final String after = "fghqwfg<caret>he asd fghzxfg<caret>hc rty fghfgfg<caret>hh vbn";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextBeforeCursorOverlapRange() {
    final String before = "<caret>q<caret>we asd zxc rty <caret>fgh vbn";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(16, 19), SelectionType.CHARACTER_WISE, false);
    typeText(parseKeys("P"));
    final String after = "fg<caret>hqfg<caret>hwe asd zxc rty fg<caret>hfgh vbn";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextAfterCursor() {
    final String before = "<caret>qwe asd <caret>zxc rty <caret>fgh vbn";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(16, 19), SelectionType.CHARACTER_WISE, false);
    typeText(parseKeys("p", "3l", "2p"));
    final String after = "qfghwe fghfg<caret>hasd zfghxc fghfg<caret>hrty ffghgh fghfg<caret>hvbn";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextAfterCursorOverlapRange() {
    final String before = "<caret>q<caret>we asd zxc rty <caret>fgh vbn";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(16, 19), SelectionType.CHARACTER_WISE, false);
    typeText(parseKeys("2p"));
    final String after = "qfghfg<caret>hwfghfg<caret>he asd zxc rty ffghfg<caret>hgh vbn";
    assertState(after);
  }


  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextBeforeCursorLinewise() {
    final String before = "q<caret>werty\n" + "as<caret>dfgh\n" + "<caret>zxcvbn\n";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(14, 21), SelectionType.LINE_WISE, false);
    typeText(parseKeys("P"));
    final String after =
      "<caret>zxcvbn\n" + "qwerty\n" + "<caret>zxcvbn\n" + "asdfgh\n" + "<caret>zxcvbn\n" + "zxcvbn\n";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextBeforeCursorLinewiseOverlapRange() {
    // Non-ide insert will produce double "<caret>zxcvbn\n"
    testPutOverlapLine("q<caret>we<caret>rty\n" + "asdfgh\n" + "<caret>zxcvbn\n",
                       "<caret>zxcvbn\n" + "qwerty\n" + "asdfgh\n" + "<caret>zxcvbn\n" + "zxcvbn\n", true);
    testPutOverlapLine("qwerty\n" + "a<caret>sd<caret>fgh\n" + "<caret>zxcvbn\n",
                       "qwerty\n" + "<caret>zxcvbn\n" + "asdfgh\n" + "<caret>zxcvbn\n" + "zxcvbn\n", true);
    testPutOverlapLine("qwerty\n" + "asd<caret>fgh\n" + "<caret>zxcvb<caret>n\n",
                       "qwerty\n" + "<caret>zxcvbn\n" + "asdfgh\n" + "<caret>zxcvbn\n" + "zxcvbn\n", true);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextAfterCursorLinewiseOverlapRange() {
    // Non-ide insert will produce double "<caret>zxcvbn\n"
    testPutOverlapLine("q<caret>wert<caret>y\n" + "asdfgh\n" + "<caret>zxcvbn\n",
                       "qwerty\n" + "<caret>zxcvbn\n" + "asdfgh\n" + "zxcvbn\n" + "<caret>zxcvbn\n", false);
    testPutOverlapLine("qwerty\n" + "as<caret>dfg<caret>h\n" + "<caret>zxcvbn\n",
                       "qwerty\n" + "asdfgh\n" + "<caret>zxcvbn\n" + "zxcvbn\n" + "<caret>zxcvbn\n", false);
    testPutOverlapLine("qwerty\n" + "asdfg<caret>h\n" + "<caret>zxcv<caret>bn\n",
                       "qwerty\n" + "asdfgh\n" + "<caret>zxcvbn\n" + "zxcvbn\n" + "<caret>zxcvbn\n", false);
  }

  private void testPutOverlapLine(@NotNull String before, @NotNull String after, boolean beforeCursor) {
    Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(14, 21), SelectionType.LINE_WISE, false);
    typeText(parseKeys(beforeCursor ? "P" : "p"));
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextAfterCursorLinewise() {
    final String before = "q<caret>werty\n" + "as<caret>dfgh\n" + "<caret>zxcvbn\n";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(14, 21), SelectionType.LINE_WISE, false);
    typeText(parseKeys("p"));
    final String after =
      "qwerty\n" + "<caret>zxcvbn\n" + "asdfgh\n" + "<caret>zxcvbn\n" + "zxcvbn\n" + "<caret>zxcvbn\n";
    assertState(after);
  }


  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextBeforeCursorMoveCursor() {
    final String before = "qw<caret>e asd z<caret>xc rty <caret>fgh vbn";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(16, 19), SelectionType.CHARACTER_WISE, false);
    typeText(parseKeys("l", "gP", "b", "gP"));
    final String after = "fgh<caret>qwefgh asd fgh<caret>zxfghc rty fgh<caret>ffghgh vbn";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextAfterCursorMoveCursor() {
    final String before = "qw<caret>e asd z<caret>xc rty <caret>fgh vbn";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(16, 19), SelectionType.CHARACTER_WISE, false);
    typeText(parseKeys("l", "gp", "b", "gp"));
    final String after = "qwe ffgh<caret>ghasd zfgh<caret>xcfgh rty ffgh<caret>gfghh vbn";
    assertState(after);
  }


  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextBeforeCursorMoveCursorLinewise() {
    final String before = "qwert<caret>y\n" + "<caret>asdfgh\n" + "zxc<caret>vbn\n";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(14, 21), SelectionType.LINE_WISE, false);
    typeText(parseKeys("gP"));
    final String after =
      "zxcvbn\n" + "<caret>qwerty\n" + "zxcvbn\n" + "<caret>asdfgh\n" + "zxcvbn\n" + "<caret>zxcvbn\n";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextAfterCursorMoveCursorLinewise() {
    final String before = "qwert<caret>y\n" + "<caret>asdfgh\n" + "zxc<caret>vbn\n";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(14, 21), SelectionType.LINE_WISE, false);
    typeText(parseKeys("gp"));
    final String after =
      "qwerty\n" + "zxcvbn\n" + "<caret>asdfgh\n" + "zxcvbn\n" + "<caret>zxcvbn\n" + "zxcvbn\n<caret>";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextBeforeCursorWithIndention() {
    final String before = "class C {\n" +
                          "    C(int i) {\n" +
                          "        myI = i;\n" +
                          "    }\n" +
                          "    <caret>private int myI = 0;\n" +
                          "    {\n" +
                          "        <caret>private int myJ = 0;\n" +
                          "    }\n" +
                          "    <caret>private int myK = 0;\n" +
                          "}";

    final Editor editor = configureByJavaText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(118, 139), SelectionType.LINE_WISE, false);
    typeText(parseKeys("P"));

    final String after = "class C {\n" +
                         "    C(int i) {\n" +
                         "        myI = i;\n" +
                         "    }\n" +
                         "    <caret>private int myK = 0;\n" +
                         "    private int myI = 0;\n" +
                         "    {\n" +
                         "        <caret>private int myK = 0;\n" +
                         "        private int myJ = 0;\n" +
                         "    }\n" +
                         "    <caret>private int myK = 0;\n" +
                         "    private int myK = 0;\n" +
                         "}";

    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextAfterCursorWithIndention() {
    final String before = "class C {\n" +
                          "    C(int i) {\n" +
                          "        myI = i;\n" +
                          "    }\n" +
                          "    <caret>private int myI = 0;\n" +
                          "    {\n" +
                          "        <caret>private int myJ = 0;\n" +
                          "    }\n" +
                          "    <caret>private int myK = 0;\n" +
                          "}";

    final Editor editor = configureByJavaText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(118, 139), SelectionType.LINE_WISE, false);
    typeText(parseKeys("p"));

    final String after = "class C {\n" +
                         "    C(int i) {\n" +
                         "        myI = i;\n" +
                         "    }\n" +
                         "    private int myI = 0;\n" +
                         "    <caret>private int myK = 0;\n" +
                         "    {\n" +
                         "        private int myJ = 0;\n" +
                         "        <caret>private int myK = 0;\n" +
                         "    }\n" +
                         "    private int myK = 0;\n" +
                         "    <caret>private int myK = 0;\n" +
                         "}";

    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextBeforeCursorBlockwise() {
    final String before = " *<caret> on<caret>e\n" + " * two\n";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister()
      .storeText(editor, new TextRange(new int[]{0, 7}, new int[]{2, 9}), SelectionType.BLOCK_WISE, false);
    typeText(parseKeys("p"));
    final String after = " * <caret> *one<caret> *\n" + " *  *two *\n";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutTextAfterCursorBlockwise() {
    final String before = " *<caret> on<caret>e\n" + " * two\n";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister()
      .storeText(editor, new TextRange(new int[]{0, 7}, new int[]{2, 8}), SelectionType.BLOCK_WISE, false);
    typeText(parseKeys("P"));
    final String after = " *<caret> * on<caret> *e\n" + " *   tw  o\n";
    assertState(after);
  }


  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testPutLinewiseWithoutLineSeparatorAtTheEndOfFile() {
    final String before = "qwe\nasd\nz<caret>xc\nrty\nfg<caret>h\nvb<caret>n";
    final Editor editor = configureByText(before);
    VimPlugin.getRegister().storeText(editor, new TextRange(0, 3), SelectionType.LINE_WISE, false);
    typeText(parseKeys("p"));
    final String after = "qwe\nasd\nzxc\n<caret>qwe\nrty\nfgh\n<caret>qwe\nvbn\n<caret>qwe\n";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testYankMotion() {
    final String before = "qwe <caret>asd <caret>zxc";
    configureByText(before);
    typeText(parseKeys("ye"));

    final Register lastRegister = VimPlugin.getRegister().getLastRegister();
    assertNotNull(lastRegister);
    final String text = lastRegister.getText();
    assertNotNull(text);

    typeText(parseKeys("P"));
    final String after = "qwe <caret>asdasd <caret>asdzxc\n" + "    zxc    zxc";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testYankMotionLineWise() {
    final String before = "<caret>qwe\n" + "rty\n" + "asd\n" + "<caret>fgh\n" + "zxc\n" + "vbn\n";
    configureByText(before);
    typeText(parseKeys("yj"));

    final Register lastRegister = VimPlugin.getRegister().getLastRegister();
    assertNotNull(lastRegister);
    final String text = lastRegister.getText();
    assertNotNull(text);

    typeText(parseKeys("P"));
    final String after = "<caret>qwe\n" +
                         "rty\n" +
                         "fgh\n" +
                         "zxc\n" +
                         "qwe\n" +
                         "rty\n" +
                         "asd\n" +
                         "<caret>qwe\n" +
                         "rty\n" +
                         "fgh\n" +
                         "zxc\n" +
                         "fgh\n" +
                         "zxc\n" +
                         "vbn\n";
    assertState(after);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.MULTICARET)
  public void testYankLine() {
    final String before = "<caret>qwe\n" + "asd\n" + "zxc\n" + "<caret>rty\n" + "fgh\n" + "vbn\n";
    configureByText(before);
    typeText(parseKeys("2yy"));

    final Register lastRegister = VimPlugin.getRegister().getLastRegister();
    assertNotNull(lastRegister);
    final String text = lastRegister.getText();
    assertNotNull(text);

    typeText(parseKeys("j", "p"));
    final String after = "qwe\n" +
                         "asd\n" +
                         "<caret>qwe\n" +
                         "asd\n" +
                         "rty\n" +
                         "fgh\n" +
                         "zxc\n" +
                         "rty\n" +
                         "fgh\n" +
                         "<caret>qwe\n" +
                         "asd\n" +
                         "rty\n" +
                         "fgh\n" +
                         "vbn\n";

    assertState(after);
  }
}

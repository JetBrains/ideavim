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

package org.jetbrains.plugins.ideavim.ex;

import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author Tuomas Tynkkynen
 */
public class RangeTest extends VimTestCase {
  public void testNoRange() {
    myFixture.configureByText("a.txt", "1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys("d"));
    myFixture.checkResult("1\n2\n4\n5\n");
  }

  public void testCurrentLine() {
    myFixture.configureByText("a.txt", "1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys(".d"));
    myFixture.checkResult("1\n2\n4\n5\n");
  }

  public void testLastLine() {
    myFixture.configureByText("a.txt", "1\n2\n3\n4\n5");
    typeText(commandToKeys("$s/5/x/"));
    myFixture.checkResult("1\n2\n3\n4\nx");
  }

  public void testOneLineNumber() {
    myFixture.configureByText("a.txt", "1\n2\n3\n4\n5\n");
    typeText(commandToKeys("3d"));
    myFixture.checkResult("1\n2\n4\n5\n");
  }

  public void testPositiveOffset() {
    myFixture.configureByText("a.txt", "1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys(".+1d"));
    myFixture.checkResult("1\n2\n3\n5\n");
  }

  public void testNegativeOffset() {
    myFixture.configureByText("a.txt", "1\n2\n3\n4\n5\n");
    typeText(commandToKeys("$-2d"));
    myFixture.checkResult("1\n2\n3\n5\n");
  }

  public void testOffsetWithNoNumber() {
    myFixture.configureByText("a.txt", "1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys(".+d"));
    myFixture.checkResult("1\n2\n3\n5\n");
  }

  public void testTwoOffsetsWithSameSign() {
    myFixture.configureByText("a.txt", "1\n<caret>2\n3\n4\n5\n");
    typeText(commandToKeys(".+1+1d"));
    myFixture.checkResult("1\n2\n3\n5\n");
  }

  public void testTwoOffsetsWithDifferentSign() {
    myFixture.configureByText("a.txt", "1\n<caret>2\n3\n4\n5\n");
    typeText(commandToKeys(".+2-1d"));
    myFixture.checkResult("1\n2\n4\n5\n");
  }

  public void testSearchForward() {
    myFixture.configureByText("a.txt", "c\na\n<caret>b\nc\nd\ne\n");
    typeText(commandToKeys("/c/d"));
    myFixture.checkResult("c\na\nb\nd\ne\n");
  }

  public void testSearchBackward() {
    myFixture.configureByText("a.txt", "c\na\n<caret>b\nc\nd\ne\n");
    typeText(commandToKeys("?c?d"));
    myFixture.checkResult("a\nb\nc\nd\ne\n");
  }

  public void testSearchWithBackslashInPattern() {
    myFixture.configureByText("a.txt", "+ add\n<caret>- sub\n/ div\n* mul\n");
    typeText(commandToKeys("/\\/ div/d"));
    myFixture.checkResult("+ add\n- sub\n* mul\n");
  }

  public void testAllLinesRange() {
    myFixture.configureByText("a.txt", "1\n2\n3\n4\n5\n");
    typeText(commandToKeys("%d"));
    myFixture.checkResult("");
  }

  public void testMultipleLineNumbersRange() {
    myFixture.configureByText("a.txt", "1\n2\n3\n4\n5\n");
    typeText(commandToKeys("2,4d"));
    myFixture.checkResult("1\n5\n");
  }

  public void testMultipleLineNumbersWithOffsetInFirst() {
    myFixture.configureByText("a.txt", "<caret>1\n2\n3\n4\n5\n");
    typeText(commandToKeys(".+1,4d"));
    myFixture.checkResult("1\n5\n");
  }

  public void testMultipleLineNumbersWithOffsetInSecond() {
    myFixture.configureByText("a.txt", "1\n2\n3\n4\n5\n");
    typeText(commandToKeys("2,$-1d"));
    myFixture.checkResult("1\n");
  }

  public void testSearchStartPositionWithComma() {
    myFixture.configureByText("a.txt", "1\n2\n3\n4\n5\n");
    typeText(commandToKeys("/2/,/[0-9]/d"));
    myFixture.checkResult("1\n3\n4\n5\n");
  }

  public void testSearchStartPositionWithSemicolon() {
    myFixture.configureByText("a.txt", "1\n2\n3\n4\n5\n");
    typeText(commandToKeys("/2/;/[0-9]/d"));
    myFixture.checkResult("1\n4\n5\n");
  }

  public void testMultipleSearches() {
    myFixture.configureByText("a.txt", "a\nfoo\nbar\nfoo\nbar\nbaz\n");
    typeText(commandToKeys("/bar//foo/d"));
    myFixture.checkResult("a\nfoo\nbar\nbar\nbaz\n");
  }
}

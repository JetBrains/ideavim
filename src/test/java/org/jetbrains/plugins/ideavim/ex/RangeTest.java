/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex;

import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author Tuomas Tynkkynen
 */
public class RangeTest extends VimTestCase {
  public void testNoRange() {
    configureByText("1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys("d"));
    assertState("1\n2\n4\n5\n");
  }

  public void testCurrentLine() {
    configureByText("1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys(".d"));
    assertState("1\n2\n4\n5\n");
  }

  public void testLastLine() {
    configureByText("1\n2\n3\n4\n5");
    typeText(commandToKeys("$s/5/x/"));
    assertState("1\n2\n3\n4\nx");
  }

  public void testOneLineNumber() {
    configureByText("1\n2\n3\n4\n5\n");
    typeText(commandToKeys("3d"));
    assertState("1\n2\n4\n5\n");
  }

  public void testPositiveOffset() {
    configureByText("1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys(".+1d"));
    assertState("1\n2\n3\n5\n");
  }

  public void testNegativeOffset() {
    configureByText("1\n2\n3\n4\n5\n");
    typeText(commandToKeys("$-2d"));
    assertState("1\n2\n3\n5\n");
  }

  public void testOffsetWithNoNumber() {
    configureByText("1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys(".+d"));
    assertState("1\n2\n3\n5\n");
  }

  public void testOffsetWithoutPlusSign() {
    // Not part of the documentation, but it works in Vim - essentially the same as ":.+2d"
    configureByText("1\n<caret>2\n3\n4\n5\n");
    typeText(commandToKeys(".2d"));
    assertState("1\n2\n3\n5\n");
  }

  public void testOffsetWithZero() {
    configureByText("1\n2\n<caret>3\n4\n5\n");
    typeText(commandToKeys(".+0d"));
    assertState("1\n2\n4\n5\n");
  }

  public void testTwoOffsetsWithSameSign() {
    configureByText("1\n<caret>2\n3\n4\n5\n");
    typeText(commandToKeys(".+1+1d"));
    assertState("1\n2\n3\n5\n");
  }

  public void testTwoOffsetsWithDifferentSign() {
    configureByText("1\n<caret>2\n3\n4\n5\n");
    typeText(commandToKeys(".+2-1d"));
    assertState("1\n2\n4\n5\n");
  }

  public void testMultipleZeroOffsets() {
    configureByText("1\n<caret>2\n3\n4\n5\n");
    typeText(commandToKeys(".+0-0d"));
    assertState("1\n3\n4\n5\n");
  }

  public void testSearchForward() {
    configureByText("c\na\n<caret>b\nc\nd\ne\n");
    typeText(commandToKeys("/c/d"));
    assertState("c\na\nb\nd\ne\n");
  }

  public void testSearchBackward() {
    configureByText("c\na\n<caret>b\nc\nd\ne\n");
    typeText(commandToKeys("?c?d"));
    assertState("a\nb\nc\nd\ne\n");
  }

  public void testSearchWithBackslashInPattern() {
    configureByText("+ add\n<caret>- sub\n/ div\n* mul\n");
    typeText(commandToKeys("/\\/ div/d"));
    assertState("+ add\n- sub\n* mul\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAllLinesRange() {
    configureByText("1\n2\n3\n4\n5\n");
    typeText(commandToKeys("%d"));
    assertState("");
  }

  public void testMultipleLineNumbersRange() {
    configureByText("1\n2\n3\n4\n5\n");
    typeText(commandToKeys("2,4d"));
    assertState("1\n5\n");
  }

  public void testMultipleLineNumbersWithOffsetInFirst() {
    configureByText("<caret>1\n2\n3\n4\n5\n");
    typeText(commandToKeys(".+1,4d"));
    assertState("1\n5\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT, description = "idk")
  public void testMultipleLineNumbersWithOffsetInSecond() {
    configureByText("1\n2\n3\n4\n5\n");
    typeText(commandToKeys("2,$-1d"));
    assertState("1\n");
  }

  public void testSearchStartPositionWithComma() {
    configureByText("1\n2\n3\n4\n5\n");
    typeText(commandToKeys("/2/,/[0-9]/d"));
    assertState("1\n3\n4\n5\n");
  }

  public void testSearchStartPositionWithSemicolon() {
    configureByText("1\n2\n3\n4\n5\n");
    typeText(commandToKeys("/2/;/[0-9]/d"));
    assertState("1\n4\n5\n");
  }

  public void testMultipleSearches() {
    configureByText("a\nfoo\nbar\nfoo\nbar\nbaz\n");
    typeText(commandToKeys("/bar//foo/d"));
    assertState("a\nfoo\nbar\nbar\nbaz\n");
  }
}

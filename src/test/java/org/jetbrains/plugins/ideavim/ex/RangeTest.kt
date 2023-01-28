/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class RangeTest : VimTestCase() {
  fun testNoRange() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys("d"))
    assertState("1\n2\n4\n5\n")
  }

  fun testCurrentLine() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys(".d"))
    assertState("1\n2\n4\n5\n")
  }

  fun testLastLine() {
    configureByText("1\n2\n3\n4\n5")
    typeText(commandToKeys("\$s/5/x/"))
    assertState("1\n2\n3\n4\nx")
  }

  fun testOneLineNumber() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("3d"))
    assertState("1\n2\n4\n5\n")
  }

  fun testPositiveOffset() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys(".+1d"))
    assertState("1\n2\n3\n5\n")
  }

  fun testNegativeOffset() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("$-2d"))
    assertState("1\n2\n3\n5\n")
  }

  fun testOffsetWithNoNumber() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys(".+d"))
    assertState("1\n2\n3\n5\n")
  }

  fun testOffsetWithoutPlusSign() {
    // Not part of the documentation, but it works in Vim - essentially the same as ":.+2d"
    configureByText("1\n<caret>2\n3\n4\n5\n")
    typeText(commandToKeys(".2d"))
    assertState("1\n2\n3\n5\n")
  }

  fun testOffsetWithZero() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys(".+0d"))
    assertState("1\n2\n4\n5\n")
  }

  fun testTwoOffsetsWithSameSign() {
    configureByText("1\n<caret>2\n3\n4\n5\n")
    typeText(commandToKeys(".+1+1d"))
    assertState("1\n2\n3\n5\n")
  }

  fun testTwoOffsetsWithDifferentSign() {
    configureByText("1\n<caret>2\n3\n4\n5\n")
    typeText(commandToKeys(".+2-1d"))
    assertState("1\n2\n4\n5\n")
  }

  fun testMultipleZeroOffsets() {
    configureByText("1\n<caret>2\n3\n4\n5\n")
    typeText(commandToKeys(".+0-0d"))
    assertState("1\n3\n4\n5\n")
  }

  fun testSearchForward() {
    configureByText("c\na\n<caret>b\nc\nd\ne\n")
    typeText(commandToKeys("/c/d"))
    assertState("c\na\nb\nd\ne\n")
  }

  fun testSearchBackward() {
    configureByText("c\na\n<caret>b\nc\nd\ne\n")
    typeText(commandToKeys("?c?d"))
    assertState("a\nb\nc\nd\ne\n")
  }

  fun testSearchWithBackslashInPattern() {
    configureByText("+ add\n<caret>- sub\n/ div\n* mul\n")
    typeText(commandToKeys("/\\/ div/d"))
    assertState("+ add\n- sub\n* mul\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testAllLinesRange() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("%d"))
    assertState("")
  }

  fun testMultipleLineNumbersRange() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("2,4d"))
    assertState("1\n5\n")
  }

  fun testMultipleLineNumbersWithOffsetInFirst() {
    configureByText("<caret>1\n2\n3\n4\n5\n")
    typeText(commandToKeys(".+1,4d"))
    assertState("1\n5\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT, description = "idk")
  fun testMultipleLineNumbersWithOffsetInSecond() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("2,$-1d"))
    assertState("1\n")
  }

  fun testSearchStartPositionWithComma() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("/2/,/[0-9]/d"))
    assertState("1\n3\n4\n5\n")
  }

  fun testSearchStartPositionWithSemicolon() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("/2/;/[0-9]/d"))
    assertState("1\n4\n5\n")
  }

  fun testMultipleSearches() {
    configureByText("a\nfoo\nbar\nfoo\nbar\nbaz\n")
    typeText(commandToKeys("/bar//foo/d"))
    assertState("a\nfoo\nbar\nbar\nbaz\n")
  }
}
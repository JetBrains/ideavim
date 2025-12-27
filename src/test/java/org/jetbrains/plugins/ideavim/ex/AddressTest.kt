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
import org.junit.jupiter.api.Test

class AddressTest : VimTestCase() {
  @Test
  fun testNoRange() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys("d"))
    assertState("1\n2\n4\n5\n")
  }

  @Test
  fun testCurrentLine() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys(".d"))
    assertState("1\n2\n4\n5\n")
  }

  @Test
  fun testLastLine() {
    configureByText("1\n2\n3\n4\n5")
    typeText(commandToKeys("\$s/5/x/"))
    assertState("1\n2\n3\n4\nx")
  }

  @Test
  fun testOneLineNumber() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("3d"))
    assertState("1\n2\n4\n5\n")
  }

  @Test
  fun testPositiveOffset() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys(".+1d"))
    assertState("1\n2\n3\n5\n")
  }

  @Test
  fun testNegativeOffset() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("$-2d"))
    assertState("1\n2\n3\n5\n")
  }

  @Test
  fun testOffsetWithNoNumber() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys(".+d"))
    assertState("1\n2\n3\n5\n")
  }

  @Test
  fun testOffsetWithoutPlusSign() {
    // Not part of the documentation, but it works in Vim - essentially the same as ":.+2d"
    configureByText("1\n<caret>2\n3\n4\n5\n")
    typeText(commandToKeys(".2d"))
    assertState("1\n2\n3\n5\n")
  }

  @Test
  fun testOffsetWithZero() {
    configureByText("1\n2\n<caret>3\n4\n5\n")
    typeText(commandToKeys(".+0d"))
    assertState("1\n2\n4\n5\n")
  }

  @Test
  fun testTwoOffsetsWithSameSign() {
    configureByText("1\n<caret>2\n3\n4\n5\n")
    typeText(commandToKeys(".+1+1d"))
    assertState("1\n2\n3\n5\n")
  }

  @Test
  fun testTwoOffsetsWithDifferentSign() {
    configureByText("1\n<caret>2\n3\n4\n5\n")
    typeText(commandToKeys(".+2-1d"))
    assertState("1\n2\n4\n5\n")
  }

  @Test
  fun testMultipleZeroOffsets() {
    configureByText("1\n<caret>2\n3\n4\n5\n")
    typeText(commandToKeys(".+0-0d"))
    assertState("1\n3\n4\n5\n")
  }

  @Test
  fun testSearchForward() {
    configureByText("c\na\n<caret>b\nc\nd\ne\n")
    typeText(commandToKeys("/c/d"))
    assertState("c\na\nb\nd\ne\n")
  }

  @Test
  fun testSearchBackward() {
    configureByText("c\na\n<caret>b\nc\nd\ne\n")
    typeText(commandToKeys("?c?d"))
    assertState("a\nb\nc\nd\ne\n")
  }

  @Test
  fun testSearchWithBackslashInPattern() {
    configureByText("+ add\n<caret>- sub\n/ div\n* mul\n")
    typeText(commandToKeys("/\\/ div/d"))
    assertState("+ add\n- sub\n* mul\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT, description = "IdeaVim removes all content leaving empty buffer, Vim/Neovim leaves single empty line")
  @Test
  fun testAllLinesRange() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("%d"))
    assertState("")
  }

  @Test
  fun testMultipleLineNumbersRange() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("2,4d"))
    assertState("1\n5\n")
  }

  @Test
  fun testMultipleLineNumbersWithOffsetInFirst() {
    configureByText("<caret>1\n2\n3\n4\n5\n")
    typeText(commandToKeys(".+1,4d"))
    assertState("1\n5\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT, description = "IdeaVim deletes all lines from 2 onwards, but Vim/Neovim should only delete lines 2-4")
  @Test
  fun testMultipleLineNumbersWithOffsetInSecond() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("2,$-1d"))
    assertState("1\n")
  }

  @Test
  fun testSearchStartPositionWithComma() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("/2/,/[0-9]/d"))
    assertState("1\n3\n4\n5\n")
  }

  @Test
  fun testSearchStartPositionWithSemicolon() {
    configureByText("1\n2\n3\n4\n5\n")
    typeText(commandToKeys("/2/;/[0-9]/d"))
    assertState("1\n4\n5\n")
  }

  @Test
  fun testMultipleSearches() {
    configureByText("a\nfoo\nbar\nfoo\nbar\nbaz\n")
    typeText(commandToKeys("/bar//foo/d"))
    assertState("a\nfoo\nbar\nbar\nbaz\n")
  }
}

/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.groups

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.getDoubleQuotesRangeNoPSI
import com.maddyhome.idea.vim.group.getSingleQuotesRangeNoPSI
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchGroupNoPSITests {
  @Test
  fun `test outer single quotes range for position outside string`() {
    val text = "let myVar = 'Oh, hi Mark'"
    assertNull(getSingleQuotesRangeNoPSI(text, 1, false))
  }

  @Test
  fun `test inner single quotes range for position outside string`() {
    val text = "let myVar = 'Oh, hi Mark'"
    assertNull(getSingleQuotesRangeNoPSI(text, 1, true))
  }

  @Test
  fun `test outer single quotes range for position inside string`() {
    val text = "let myVar = 'Oh, hi Mark'"
    assertEquals(TextRange(12, 25), getSingleQuotesRangeNoPSI(text, 15, false))
  }

  @Test
  fun `test inner single quotes range for position inside string`() {
    val text = "let myVar = 'Oh, hi Mark'"
    assertEquals(TextRange(13, 24), getSingleQuotesRangeNoPSI(text, 15, true))
  }

  @Test
  fun `test outer single quotes range for position at opening quote`() {
    val text = "let myVar = 'Oh, hi Mark'"
    assertEquals(TextRange(12, 25), getSingleQuotesRangeNoPSI(text, 12, false))
  }

  @Test
  fun `test inner single quotes range for position at opening quote`() {
    val text = "let myVar = 'Oh, hi Mark'"
    assertEquals(TextRange(13, 24), getSingleQuotesRangeNoPSI(text, 12, true))
  }

  @Test
  fun `test outer single quotes range for position at closing quote`() {
    val text = "let myVar = 'Oh, hi Mark'"
    assertEquals(TextRange(12, 25), getSingleQuotesRangeNoPSI(text, 24, false))
  }

  @Test
  fun `test inner single quotes range for position at closing quote`() {
    val text = "let myVar = 'Oh, hi Mark'"
    assertEquals(TextRange(13, 24), getSingleQuotesRangeNoPSI(text, 24, true))
  }

  @Test
  fun `test outer single quotes range for position after closing quote`() {
    val text = "let myVar = 'Oh, hi Mark' // comment"
    assertNull(getSingleQuotesRangeNoPSI(text, 25, false))
  }

  @Test
  fun `test inner single quotes range for position after closing quote`() {
    val text = "let myVar = 'Oh, hi Mark' // comment"
    assertNull(getSingleQuotesRangeNoPSI(text, 25, true))
  }

  @Test
  fun `test outer double quotes range for position outside string`() {
    val text = "let myVar = \"Oh, hi Mark\""
    assertNull(getDoubleQuotesRangeNoPSI(text, 1, false))
  }

  @Test
  fun `test inner double quotes range for position outside string`() {
    val text = "let myVar = \"Oh, hi Mark\""
    assertNull(getDoubleQuotesRangeNoPSI(text, 1, true))
  }

  @Test
  fun `test outer double quotes range for position inside string`() {
    val text = "let myVar = \"Oh, hi Mark\""
    assertEquals(TextRange(12, 25), getDoubleQuotesRangeNoPSI(text, 15, false))
  }

  @Test
  fun `test inner double quotes range for position inside string`() {
    val text = "let myVar = \"Oh, hi Mark\""
    assertEquals(TextRange(13, 24), getDoubleQuotesRangeNoPSI(text, 15, true))
  }

  @Test
  fun `test outer double quotes range for position at opening quote`() {
    val text = "let myVar = \"Oh, hi Mark\""
    assertEquals(TextRange(12, 25), getDoubleQuotesRangeNoPSI(text, 12, false))
  }

  @Test
  fun `test inner double quotes range for position at opening quote`() {
    val text = "let myVar = \"Oh, hi Mark\""
    assertEquals(TextRange(13, 24), getDoubleQuotesRangeNoPSI(text, 12, true))
  }

  @Test
  fun `test outer double quotes range for position at closing quote`() {
    val text = "let myVar = \"Oh, hi Mark\""
    assertEquals(TextRange(12, 25), getDoubleQuotesRangeNoPSI(text, 24, false))
  }

  @Test
  fun `test inner double quotes range for position at closing quote`() {
    val text = "let myVar = \"Oh, hi Mark\""
    assertEquals(TextRange(13, 24), getDoubleQuotesRangeNoPSI(text, 24, true))
  }

  @Test
  fun `test outer double quotes range for position after closing quote`() {
    val text = "let myVar = \"Oh, hi Mark\" // comment"
    assertNull(getDoubleQuotesRangeNoPSI(text, 25, false))
  }

  @Test
  fun `test inner double quotes range for position after closing quote`() {
    val text = "let myVar = \"Oh, hi Mark\" // comment"
    assertNull(getDoubleQuotesRangeNoPSI(text, 25, true))
  }
}
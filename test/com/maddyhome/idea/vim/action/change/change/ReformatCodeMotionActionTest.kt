/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim.action.change.change

import org.junit.Assert.assertEquals
import org.junit.Test

class ReformatCodeMotionActionTest {
  
  @Test
  fun testTextParagraphReFormatting() {
    val inputText = """Please note that daily-run does not attempt to run the backup at exactly the
time. It can be up to time adding the interval setting. For the default 4min interval this means that the command can run from 01:00:00 up to 01:04:10.
   
    
"""
    val result: ReformatCodeMotionAction.ReformatParagraphResult = ReformatCodeMotionAction.reformatTextAsParagraph(inputText, 0)
    val formattedText: String = result.text
    val expected = """Please note that daily-run does not attempt to run the backup at exactly the
time. It can be up to time adding the interval setting. For the default 4min
interval this means that the command can run from 01:00:00 up to 01:04:10.
   
    
"""
    println("inputText = \n$inputText")
    println("formattedText = \n$formattedText")
    println("expected = \n$expected")
    assertEquals(expected, formattedText)
  }

  @Test
  fun testTextParagraphReFormattingNoTrailingWhitespace() {
    val inputText = "Please note"
    val formattedText: String = ReformatCodeMotionAction.reformatTextAsParagraph(inputText, 0)
      .text
    val expected = "Please note"
    assertEquals(expected, formattedText)
  }

  @Test
  fun testTextParagraphReFormattingWhenEmpty() {
    val formattedText: String = ReformatCodeMotionAction.reformatTextAsParagraph("", 0)
      .text
    assertEquals("", formattedText)
  }

  @Test
  fun testTextParagraphReFormattingWhenBlank() {
    val formattedText: String = ReformatCodeMotionAction.reformatTextAsParagraph("    ", 0)
      .text
    assertEquals("    ", formattedText)
  }

  @Test
  fun testTextParagraphReFormattingWhenEmptyLines() {
    val formattedText: String = ReformatCodeMotionAction.reformatTextAsParagraph(" \n \n \n \n \n", 0)
      .text
    assertEquals(" \n \n \n \n \n", formattedText)
  }
}
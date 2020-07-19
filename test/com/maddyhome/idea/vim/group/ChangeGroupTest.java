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

package com.maddyhome.idea.vim.group;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChangeGroupTest {

  @Test
  public void testTextParagraphReFormatting() {
    ChangeGroup changeGroup = new ChangeGroup();

    final String inputText = "Please note that daily-run does not attempt to run the backup at exactly the\n" +
      "time. It can be up to time adding the interval setting. For the default 4min interval this means that " +
      "the command can run from 01:00:00 up to 01:04:10.\n   \n    \n";

    final String formattedText = changeGroup.reformatTextAsParagraph(inputText);

    final String expected = "Please note that daily-run does not attempt to run the backup at exactly the\n" +
      "time. It can be up to time adding the interval setting. For the default 4min\n" +
      "interval this means that the command can run from 01:00:00 up to 01:04:10.\n   \n    \n";
    System.out.println("inputText = \n" + inputText);
    System.out.println("formattedText = \n" + formattedText);
    System.out.println("expected = \n" + expected);
    assertEquals(expected, formattedText);
  }

  @Test
  public void testTextParagraphReFormattingNoTrailingWhitespace() {
    ChangeGroup changeGroup = new ChangeGroup();

    final String inputText = "Please note";

    final String formattedText = changeGroup.reformatTextAsParagraph(inputText);

    final String expected = "Please note";
    assertEquals(expected, formattedText);
  }
  @Test
  public void testTextParagraphReFormattingWhenEmpty() {
    ChangeGroup changeGroup = new ChangeGroup();

    final String formattedText = changeGroup.reformatTextAsParagraph("");

    assertEquals("", formattedText);
  }

  @Test
  public void testTextParagraphReFormattingWhenBlank() {
    ChangeGroup changeGroup = new ChangeGroup();

    final String formattedText = changeGroup.reformatTextAsParagraph("    ");

    assertEquals("    ", formattedText);
  }

  @Test
  public void testTextParagraphReFormattingWhenEmptyLines() {
    ChangeGroup changeGroup = new ChangeGroup();

    final String formattedText = changeGroup.reformatTextAsParagraph(" \n \n \n \n \n");

    assertEquals(" \n \n \n \n \n", formattedText);
  }
}
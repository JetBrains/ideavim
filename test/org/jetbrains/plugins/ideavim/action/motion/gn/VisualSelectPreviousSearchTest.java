/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action.motion.gn;

import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.EnumSet;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

public class VisualSelectPreviousSearchTest extends VimTestCase {
  public void testSearch() {
    typeTextInFile(parseKeys("*w","gN"),
            "h<caret>ello world\nhello world hello world");

    assertOffset(12);
    assertSelection("hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testSearchMulticaret() {
    typeTextInFile(parseKeys("*", "b", "gN"), "h<caret>ello world\nh<caret>ello world hello world");

    assertEquals(1, myFixture.getEditor().getCaretModel().getCaretCount());
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testSearchWhenOnMatch() {
    typeTextInFile(parseKeys("*","gN"),
                   "h<caret>ello world\nhello world hello world");

    assertOffset(12);
    assertSelection("hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testWithoutSpaces() {
    configureByText("tes<caret>ttest");
    VimPlugin.getSearch().search(myFixture.getEditor(), "test", 1, EnumSet.noneOf(CommandFlags.class), false);
    typeText(parseKeys("gN"));

    assertOffset(0);
    assertSelection("test");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testSearchTwice() {
    typeTextInFile(parseKeys("*", "2gN"), "hello world\nh<caret>ello world hello");

    assertOffset(12);
    assertSelection("hello");
  }

  public void testTwoSearchesStayInVisualMode() {
    typeTextInFile(parseKeys("*", "gN", "gN"), "hello world\nh<caret>ello world hello");

    assertOffset(12);
    assertSelection("hello world hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testCanExitVisualMode() {
    typeTextInFile(parseKeys("*", "gN", "gN", "<Esc>"), "hello world\nh<caret>ello world hello");

    assertOffset(12);
    assertSelection(null);
    assertMode(CommandState.Mode.COMMAND);
  }

  public void testIfInMiddlePositionOfSearchAndInVisualModeThenSelectCurrent() {
    typeTextInFile(parseKeys("*llv", "gN"), "hello hello");

    assertOffset(6);
    assertSelection("hel");
    assertMode(CommandState.Mode.VISUAL);
  }
}

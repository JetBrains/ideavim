package org.jetbrains.plugins.ideavim.gn;

import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

public class VisualSelectPreviousSearchTest extends VimTestCase {
  public void testSearch() {
    typeTextInFile(parseKeys("*w","gN"),
            "h<caret>ello world\nhello world hello world");

    assertOffset(12);
    assertSelection("hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testSearchWhenOnMatch() {
    typeTextInFile(parseKeys("*","gN"),
                   "h<caret>ello world\nhello world hello world");

    assertOffset(12);
    assertSelection("hello");
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

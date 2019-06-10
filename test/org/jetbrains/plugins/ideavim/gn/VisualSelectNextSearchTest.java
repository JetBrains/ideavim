package org.jetbrains.plugins.ideavim.gn;

import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;
import static org.hamcrest.core.Is.is;

public class VisualSelectNextSearchTest extends VimTestCase {
  public void testSearch() {
    typeTextInFile(parseKeys("*", "b", "gn"), "h<caret>ello world\nhello world hello world");

    assertOffset(16);
    assertSelection("hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testSearchCurrentlyInOne() {
    typeTextInFile(parseKeys("*", "gn"), "h<caret>ello world\nhello world hello world");

    assertOffset(16);
    assertSelection("hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testSearchTwice() {
    typeTextInFile(parseKeys("*", "2gn"), "h<caret>ello world\nhello world hello, hello");

    assertOffset(28);
    assertSelection("hello");
  }

  public void testTwoSearchesStayInVisualMode() {
    typeTextInFile(parseKeys("*", "gn", "gn"), "h<caret>ello world\nhello world hello, hello");

    assertOffset(28);
    assertSelection("hello world hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testCanExitVisualMode() {
    typeTextInFile(parseKeys("*", "gn", "gn", "<Esc>"), "h<caret>ello world\nhello world hello, hello");

    assertOffset(28);
    assertSelection(null);
    assertMode(CommandState.Mode.COMMAND);
  }

  public void testNullSelectionDoesNothing() {
    typeTextInFile(parseKeys("/bye<CR>", "gn"), "h<caret>ello world\nhello world hello world");

    assertOffset(1);
    assertSelection(null);
  }

  public void testIfInLastPositionOfSearchAndInNormalModeThenSelectCurrent() {
    typeTextInFile(parseKeys("*0e", "gn"), "h<caret>ello hello");

    assertOffset(4);
    assertSelection("hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testIfInMiddlePositionOfSearchAndInVisualModeThenSelectCurrent() {
    typeTextInFile(parseKeys("*0llv", "gn"), "h<caret>ello hello");

    assertOffset(4);
    assertSelection("llo");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testIfInLastPositionOfSearchAndInVisualModeThenSelectNext() {
    typeTextInFile(parseKeys("*0ev", "gn"), "h<caret>ello hello");

    assertOffset(10);
    assertSelection("o hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testMixWithN() {
    typeTextInFile(parseKeys("*", "gn", "n", "gn"), "h<caret>ello world\nhello world hello, hello");

    assertOffset(28);
    assertSelection("hello world hello");
    assertMode(CommandState.Mode.VISUAL);
  }

  public void testMixWithPreviousSearch() {
    typeTextInFile(parseKeys("*", "gn", "gn", "gN", "gn"), "h<caret>ello world\nhello world hello, hello");

    assertOffset(28);
    assertSelection("hello world hello");
    assertMode(CommandState.Mode.VISUAL);
  }
}

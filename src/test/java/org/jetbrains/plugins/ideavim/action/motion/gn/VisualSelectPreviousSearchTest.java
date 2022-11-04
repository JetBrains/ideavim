/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.gn;

import com.intellij.idea.TestFor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.VimStateMachine;
import com.maddyhome.idea.vim.common.Direction;
import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

public class VisualSelectPreviousSearchTest extends VimTestCase {
  @TestFor(classes = {SearchWholeWordForwardAction.class})
  public void testSearch() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("*w" + "gN"), "h<caret>ello world\nhello world hello world");

    assertOffset(12);
    assertSelection("hello");
    assertMode(VimStateMachine.Mode.VISUAL);
  }

  @TestFor(classes = {SearchWholeWordForwardAction.class})
  public void testSearchMulticaret() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("*" + "b" + "gN"), "h<caret>ello world\nh<caret>ello world hello world");

    assertEquals(1, myFixture.getEditor().getCaretModel().getCaretCount());
    assertMode(VimStateMachine.Mode.VISUAL);
  }

  @TestFor(classes = {SearchWholeWordForwardAction.class})
  public void testSearchWhenOnMatch() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("*" + "gN"), "h<caret>ello world\nhello world hello world");

    assertOffset(12);
    assertSelection("hello");
    assertMode(VimStateMachine.Mode.VISUAL);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithoutSpaces() {
    configureByText("tes<caret>ttest");
    VimPlugin.getSearch().setLastSearchState(myFixture.getEditor(), "test", "", Direction.FORWARDS);
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("gN"));

    assertOffset(0);
    assertSelection("test");
    assertMode(VimStateMachine.Mode.VISUAL);
  }

  @TestFor(classes = {SearchWholeWordForwardAction.class})
  public void testSearchTwice() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("*" + "2gN"), "hello world\nh<caret>ello world hello");

    assertOffset(12);
    assertSelection("hello");
  }

  @TestFor(classes = {SearchWholeWordForwardAction.class})
  public void testTwoSearchesStayInVisualMode() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("*" + "gN" + "gN"), "hello world\nh<caret>ello world hello");

    assertOffset(12);
    assertSelection("hello world hello");
    assertMode(VimStateMachine.Mode.VISUAL);
  }

  @TestFor(classes = {SearchWholeWordForwardAction.class})
  public void testCanExitVisualMode() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("*" + "gN" + "gN" + "<Esc>"), "hello world\nh<caret>ello world hello");

    assertOffset(12);
    assertSelection(null);
    assertMode(VimStateMachine.Mode.COMMAND);
  }

  @TestFor(classes = {SearchWholeWordForwardAction.class})
  public void testIfInMiddlePositionOfSearchAndInVisualModeThenSelectCurrent() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("*llv" + "gN"), "hello hello");

    assertOffset(6);
    assertSelection("hel");
    assertMode(VimStateMachine.Mode.VISUAL);
  }

  public void testWithTabs() {
    typeTextInFile(VimInjectorKt.getInjector().getParser().parseKeys("*" + "gN" + "gN"), "hello 1\n\thello 2\n\the<caret>llo 3\n\thello 4");

    assertOffset(18);
    assertSelection("hello 3\n\thello");
    assertMode(VimStateMachine.Mode.VISUAL);
  }
}

/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action.motion.gn

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.Direction
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class VisualSelectPreviousSearchTest : VimTestCase() {
  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun testSearch() {
    typeTextInFile(injector.parser.parseKeys("*w" + "gN"), "h<caret>ello world\nhello world hello world")
    assertOffset(12)
    assertSelection("hello")
    assertMode(VimStateMachine.Mode.VISUAL)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun testSearchMulticaret() {
    typeTextInFile(
      injector.parser.parseKeys("*" + "b" + "gN"),
      "h<caret>ello world\nh<caret>ello world hello world",
    )
    kotlin.test.assertEquals(1, fixture.editor.caretModel.caretCount)
    assertMode(VimStateMachine.Mode.VISUAL)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun testSearchWhenOnMatch() {
    typeTextInFile(injector.parser.parseKeys("*" + "gN"), "h<caret>ello world\nhello world hello world")
    assertOffset(12)
    assertSelection("hello")
    assertMode(VimStateMachine.Mode.VISUAL)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testWithoutSpaces() {
    configureByText("tes<caret>ttest")
    VimPlugin.getSearch().setLastSearchState(fixture.editor, "test", "", Direction.FORWARDS)
    typeText(injector.parser.parseKeys("gN"))
    assertOffset(0)
    assertSelection("test")
    assertMode(VimStateMachine.Mode.VISUAL)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun testSearchTwice() {
    typeTextInFile(injector.parser.parseKeys("*" + "2gN"), "hello world\nh<caret>ello world hello")
    assertOffset(12)
    assertSelection("hello")
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun testTwoSearchesStayInVisualMode() {
    typeTextInFile(injector.parser.parseKeys("*" + "gN" + "gN"), "hello world\nh<caret>ello world hello")
    assertOffset(12)
    assertSelection("hello world hello")
    assertMode(VimStateMachine.Mode.VISUAL)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun testCanExitVisualMode() {
    typeTextInFile(injector.parser.parseKeys("*" + "gN" + "gN" + "<Esc>"), "hello world\nh<caret>ello world hello")
    assertOffset(12)
    assertSelection(null)
    assertMode(VimStateMachine.Mode.COMMAND)
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun testIfInMiddlePositionOfSearchAndInVisualModeThenSelectCurrent() {
    typeTextInFile(injector.parser.parseKeys("*llv" + "gN"), "hello hello")
    assertOffset(6)
    assertSelection("hel")
    assertMode(VimStateMachine.Mode.VISUAL)
  }

  @Test
  fun testWithTabs() {
    typeTextInFile(injector.parser.parseKeys("*" + "gN" + "gN"), "hello 1\n\thello 2\n\the<caret>llo 3\n\thello 4")
    assertOffset(18)
    assertSelection("hello 3\n\thello")
    assertMode(VimStateMachine.Mode.VISUAL)
  }
}

/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.Test

class RepeatActionTest : VimTestCase() {

  @Test
  fun testSimpleRepeatLastCommand() {
    configureByText("foo foo")
    typeText(injector.parser.parseKeys("cw" + "bar" + "<Esc>" + "w" + "."))
    assertState("bar bar")
  }

  @Test
  fun testRepeatChangeToCharInNextLine() {
    configureByText(
      "The first line.\n" +
        "This is the second line.\n" +
        "Third line here, with a comma.\n" +
        "Last line."
    )
    typeText(injector.parser.parseKeys("j" + "ct." + "Change the line to point" + "<Esc>" + "j0" + "."))
    assertState(
      "The first line.\n" +
        "Change the line to point.\n" +
        "Change the line to point.\n" +
        "Last line."
    )
  }

  // VIM-1644
  @Test
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testRepeatChangeInVisualMode() {
    configureByText("foobar foobar")
    typeText(injector.parser.parseKeys("<C-V>llc" + "fu" + "<Esc>" + "w" + "."))
    assertState("fubar fubar")
  }

  // VIM-1644
  @Test
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testRepeatChangeInVisualModeMultiline() {
    configureByText(
      "There is a red house.\n" +
        "Another red house there.\n" +
        "They have red windows.\n" +
        "Good."
    )
    typeText(injector.parser.parseKeys("www" + "<C-V>ec" + "blue" + "<Esc>" + "j0w." + "j0ww."))
    assertState(
      "There is a blue house.\n" +
        "Another blue house there.\n" +
        "They have blue windows.\n" +
        "Good."
    )
  }
}

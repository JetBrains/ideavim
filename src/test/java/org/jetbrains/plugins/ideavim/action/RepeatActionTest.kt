/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.key
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingOwner
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

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
        "Last line.",
    )
    typeText(injector.parser.parseKeys("j" + "ct." + "Change the line to point" + "<Esc>" + "j0" + "."))
    assertState(
      "The first line.\n" +
        "Change the line to point.\n" +
        "Change the line to point.\n" +
        "Last line.",
    )
  }

  // VIM-1644
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testRepeatChangeInVisualMode() {
    configureByText("foobar foobar")
    typeText(injector.parser.parseKeys("<C-V>llc" + "fu" + "<Esc>" + "w" + "."))
    assertState("fubar fubar")
  }

  // VIM-1644
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testRepeatChangeInVisualModeMultiline() {
    configureByText(
      "There is a red house.\n" +
        "Another red house there.\n" +
        "They have red windows.\n" +
        "Good.",
    )
    typeText(injector.parser.parseKeys("www" + "<C-V>ec" + "blue" + "<Esc>" + "j0w." + "j0ww."))
    assertState(
      "There is a blue house.\n" +
        "Another blue house there.\n" +
        "They have blue windows.\n" +
        "Good.",
    )
  }

  @Test
  fun `repeat delete command`() {
    doTest(
      "i<del><esc>.",
      "${c}1234567890",
      "${c}34567890"
    )
  }

  @Test
  fun `map for the delete`() {
    doTest(
      "i<del><esc>.",
      """
      Lorem Ipsum

      ${c}Lorem ipsum dolor sit amet,
      consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
      Lorem Ipsum

      ${c}jjLorem ipsum dolor sit amet,
      consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
      """.trimIndent()
    ) {
      injector.keyGroup.putKeyMapping(
        MappingMode.I,
        listOf(key("<DEL>")),
        MappingOwner.IdeaVim.Other,
        listOf(key("j")),
        false
      )
    }
  }

  @Test
  @Disabled("This test throws `Recursive runForEachCaret invocations are not allowed`")
  fun `repeat command with execution of ij action`() {
    doTest(
      "c<C-End><C-I><esc>.",
      """
        ${c}1234567890
        ${c}1234567890
        """.trimIndent(),
      """
        ${c}1234567890
        ${c}1234567890
        """.trimIndent()
    )
  }
}

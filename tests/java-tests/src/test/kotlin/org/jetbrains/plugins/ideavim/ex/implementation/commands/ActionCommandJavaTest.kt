/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class ActionCommandJavaTest : VimJavaTestCase() {
  // VIM-862 |:action| in visual character mode
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testExCommandInVisualCharacterMode() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----",
    )
    typeText("vjl")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(Mode.NORMAL())
    assertState(
      "-----\n" +
        "1/*2345\n" +
        "abc*/de\n" +
        "-----",
    )
  }

  // https://github.com/JetBrains/ideavim/commit/fe714a90032d0cb5ef0a0e0d8783980b6f1c7d20#r35647600
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testExCommandInVisualCharacterModeWithIncSearch() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----",
    )
    enterCommand("set incsearch")
    typeText("vjl")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(Mode.NORMAL())
    assertState(
      "-----\n" +
        "1/*2345\n" +
        "abc*/de\n" +
        "-----",
    )
  }

  // VIM-862 |:action|
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testExCommandInVisualCharacterModeSameLine() {
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    typeText("vl")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(Mode.NORMAL())
    assertState("1/*23*/45\n" + "abcde\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testExCommandInVisualCharacterModeSameLineWithIncsearch() {
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    enterCommand("set incsearch")
    typeText("vl")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(Mode.NORMAL())
    assertState("1/*23*/45\n" + "abcde\n")
  }

  // VIM-862 |:action| in visual line mode
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testExCommandInVisualLineMode() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----",
    )
    typeText("Vj")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(Mode.NORMAL())
    assertState(
      "-----\n" +
        "/*\n" +
        "12345\n" +
        "abcde\n" +
        "*/\n" +
        "-----",
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testExCommandInVisualLineModeWithIncsearch() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----",
    )
    enterCommand("incsearch")
    typeText("Vj")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(Mode.NORMAL())
    assertState(
      "-----\n" +
        "/*\n" +
        "12345\n" +
        "abcde\n" +
        "*/\n" +
        "-----",
    )
  }

  // VIM-862 |:action| in visual block mode
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testExCommandInVisualBlockMode() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----",
    )
    typeText("<C-V>lj")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(Mode.NORMAL())
    assertState(
      "-----\n" +
        "1/*23*/45\n" +
        "a/*bc*/de\n" +
        "-----",
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testExCommandInVisualBlockModeWithIncsearch() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----",
    )
    enterCommand("set incsearch")
    typeText("<C-V>lj")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(Mode.NORMAL())
    assertState(
      "-----\n" +
        "1/*23*/45\n" +
        "a/*bc*/de\n" +
        "-----",
    )
  }
}
/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */

class ActionCommandTest : VimTestCase() {
  // VIM-652 |:action|
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testEditorRightAction() {
    configureByText("<caret>foo\n" + "bar\n")
    enterCommand("action EditorRight")
    assertMode(VimStateMachine.Mode.COMMAND)
    assertState("f<caret>oo\n" + "bar\n")
  }

  // VIM-862 |:action| in visual character mode
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualCharacterMode() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText("vjl")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*2345\n" +
        "abc*/de\n" +
        "-----"
    )
  }

  // https://github.com/JetBrains/ideavim/commit/fe714a90032d0cb5ef0a0e0d8783980b6f1c7d20#r35647600
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualCharacterModeWithIncSearch() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    enterCommand("set incsearch")
    typeText("vjl")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*2345\n" +
        "abc*/de\n" +
        "-----"
    )
  }

  // VIM-862 |:action|
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualCharacterModeSameLine() {
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    typeText("vl")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState("1/*23*/45\n" + "abcde\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualCharacterModeSameLineWithIncsearch() {
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    enterCommand("set incsearch")
    typeText("vl")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState("1/*23*/45\n" + "abcde\n")
  }

  // VIM-862 |:action| in visual line mode
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualLineMode() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText("Vj")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "/*\n" +
        "12345\n" +
        "abcde\n" +
        "*/\n" +
        "-----"
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualLineModeWithIncsearch() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    enterCommand("incsearch")
    typeText("Vj")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "/*\n" +
        "12345\n" +
        "abcde\n" +
        "*/\n" +
        "-----"
    )
  }

  // VIM-862 |:action| in visual block mode
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualBlockMode() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText("<C-V>lj")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*23*/45\n" +
        "a/*bc*/de\n" +
        "-----"
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualBlockModeWithIncsearch() {
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    enterCommand("set incsearch")
    typeText("<C-V>lj")
    enterCommand("'<,'>action CommentByBlockComment")
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*23*/45\n" +
        "a/*bc*/de\n" +
        "-----"
    )
  }
}

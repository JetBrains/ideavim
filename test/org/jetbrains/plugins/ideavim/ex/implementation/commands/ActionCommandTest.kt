/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
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
    typeText(commandToKeys("action EditorRight"))
    assertMode(CommandState.Mode.COMMAND)
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
    typeText(parseKeys("vjl"))
    // todo add '<,'>
    typeText(commandToKeys("action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
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
    OptionsManager.incsearch.set()
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText(parseKeys("vjl"))
    // todo add '<,'>
    typeText(commandToKeys("action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*2345\n" +
        "abc*/de\n" +
        "-----"
    )
    OptionsManager.incsearch.reset()
  }

  // VIM-862 |:action|
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualCharacterModeSameLine() {
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    typeText(parseKeys("vl"))
    // todo add '<,'>
    typeText(commandToKeys("action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    assertState("1/*23*/45\n" + "abcde\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualCharacterModeSameLineWithIncsearch() {
    OptionsManager.incsearch.set()
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    typeText(parseKeys("vl"))
    // todo add '<,'>
    typeText(commandToKeys("action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    assertState("1/*23*/45\n" + "abcde\n")
    OptionsManager.incsearch.reset()
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
    typeText(parseKeys("Vj"))
    // todo add '<,'>
    typeText(commandToKeys("action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
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
    OptionsManager.incsearch.set()
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText(parseKeys("Vj"))
    // todo add '<,'>
    typeText(commandToKeys("action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    assertState(
      "-----\n" +
        "/*\n" +
        "12345\n" +
        "abcde\n" +
        "*/\n" +
        "-----"
    )
    OptionsManager.incsearch.reset()
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
    typeText(parseKeys("<C-V>lj"))
    // todo add '<,'>
    typeText(commandToKeys("action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*23*/45\n" +
        "a/*bc*/de\n" +
        "-----"
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualBlockModeWithIncsearch() {
    OptionsManager.incsearch.set()
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText(parseKeys("<C-V>lj"))
    // todo add '<,'>
    typeText(commandToKeys("action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*23*/45\n" +
        "a/*bc*/de\n" +
        "-----"
    )
    OptionsManager.incsearch.reset()
  }
}

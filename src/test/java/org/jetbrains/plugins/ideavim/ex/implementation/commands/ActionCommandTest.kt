/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
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
    typeText(injector.parser.parseKeys("vjl"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
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
    VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, OptionConstants.incsearchName)
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText(injector.parser.parseKeys("vjl"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*2345\n" +
        "abc*/de\n" +
        "-----"
    )
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.incsearchName)
  }

  // VIM-862 |:action|
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualCharacterModeSameLine() {
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    typeText(injector.parser.parseKeys("vl"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState("1/*23*/45\n" + "abcde\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  fun testExCommandInVisualCharacterModeSameLineWithIncsearch() {
    VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, OptionConstants.incsearchName)
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    typeText(injector.parser.parseKeys("vl"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState("1/*23*/45\n" + "abcde\n")
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.incsearchName)
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
    typeText(injector.parser.parseKeys("Vj"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
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
    VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, OptionConstants.incsearchName)
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText(injector.parser.parseKeys("Vj"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "/*\n" +
        "12345\n" +
        "abcde\n" +
        "*/\n" +
        "-----"
    )
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.incsearchName)
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
    typeText(injector.parser.parseKeys("<C-V>lj"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
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
    VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, OptionConstants.incsearchName)
    configureByJavaText(
      "-----\n" +
        "1<caret>2345\n" +
        "abcde\n" +
        "-----"
    )
    typeText(injector.parser.parseKeys("<C-V>lj"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(VimStateMachine.Mode.VISUAL)
    assertState(
      "-----\n" +
        "1/*23*/45\n" +
        "a/*bc*/de\n" +
        "-----"
    )
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.incsearchName)
  }
}

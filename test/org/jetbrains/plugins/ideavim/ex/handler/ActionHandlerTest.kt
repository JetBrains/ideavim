package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */

class ActionHandlerTest : VimTestCase() {
  // VIM-652 |:action|
  fun testEditorRightAction() {
    configureByText("<caret>foo\n" + "bar\n")
    typeText(commandToKeys("action EditorRight"))
    assertMode(CommandState.Mode.COMMAND)
    myFixture.checkResult("f<caret>oo\n" + "bar\n")
  }

  // VIM-862 |:action| in visual character mode
  fun testExCommandInVisualCharacterMode() {
    configureByJavaText("-----\n" +
      "1<caret>2345\n" +
      "abcde\n" +
      "-----")
    typeText(parseKeys("vjl"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult("-----\n" +
      "1/*2345\n" +
      "abc*/de\n" +
      "-----")
  }

  // https://github.com/JetBrains/ideavim/commit/fe714a90032d0cb5ef0a0e0d8783980b6f1c7d20#r35647600
  fun testExCommandInVisualCharacterModeWithIncSearch() {
    OptionsManager.incsearch.set()
    configureByJavaText("-----\n" +
      "1<caret>2345\n" +
      "abcde\n" +
      "-----")
    typeText(parseKeys("vjl"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult("-----\n" +
      "1/*2345\n" +
      "abc*/de\n" +
      "-----")
    OptionsManager.incsearch.reset()
  }

  // VIM-862 |:action|
  fun testExCommandInVisualCharacterModeSameLine() {
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    typeText(parseKeys("vl"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult("1/*23*/45\n" + "abcde\n")
  }

  fun testExCommandInVisualCharacterModeSameLineWithIncsearch() {
    OptionsManager.incsearch.set()
    configureByJavaText("1<caret>2345\n" + "abcde\n")
    typeText(parseKeys("vl"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult("1/*23*/45\n" + "abcde\n")
    OptionsManager.incsearch.reset()
  }

  // VIM-862 |:action| in visual line mode
  fun testExCommandInVisualLineMode() {
    configureByJavaText("-----\n" +
      "1<caret>2345\n" +
      "abcde\n" +
      "-----")
    typeText(parseKeys("Vj"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult("-----\n" +
      "/*\n" +
      "12345\n" +
      "abcde\n" +
      "*/\n" +
      "-----")
  }

  fun testExCommandInVisualLineModeWithIncsearch() {
    OptionsManager.incsearch.set()
    configureByJavaText("-----\n" +
      "1<caret>2345\n" +
      "abcde\n" +
      "-----")
    typeText(parseKeys("Vj"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult("-----\n" +
      "/*\n" +
      "12345\n" +
      "abcde\n" +
      "*/\n" +
      "-----")
    OptionsManager.incsearch.reset()
  }

  // VIM-862 |:action| in visual block mode
  fun testExCommandInVisualBlockMode() {
    configureByJavaText("-----\n" +
      "1<caret>2345\n" +
      "abcde\n" +
      "-----")
    typeText(parseKeys("<C-V>lj"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult("-----\n" +
      "1/*23*/45\n" +
      "a/*bc*/de\n" +
      "-----")
  }

  fun testExCommandInVisualBlockModeWithIncsearch() {
    OptionsManager.incsearch.set()
    configureByJavaText("-----\n" +
      "1<caret>2345\n" +
      "abcde\n" +
      "-----")
    typeText(parseKeys("<C-V>lj"))
    typeText(commandToKeys("'<,'>action CommentByBlockComment"))
    assertMode(CommandState.Mode.VISUAL)
    myFixture.checkResult("-----\n" +
      "1/*23*/45\n" +
      "a/*bc*/de\n" +
      "-----")
    OptionsManager.incsearch.reset()
  }
}

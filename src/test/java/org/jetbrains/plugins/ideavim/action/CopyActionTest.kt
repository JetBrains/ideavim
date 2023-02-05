/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author vlan
 */
@Suppress("SpellCheckingInspection")
class CopyActionTest : VimTestCase() {
  // |y| |p| |count|
  fun testYankPutCharacters() {
    typeTextInFile("y2h" + "p", "one two<caret> three\n")
    assertState("one twwoo three\n")
  }

  // |yy|
  fun testYankLine() {
    typeTextInFile(
      "yy" + "p",
      """
     one
     tw<caret>o
     three
     
      """.trimIndent()
    )
    assertState(
      """
    one
    two
    two
    three
    
      """.trimIndent()
    )
  }

  // VIM-723 |p|
  fun testYankPasteToEmptyLine() {
    typeTextInFile(
      "yiw" + "j" + "p",
      """
     foo
     
     bar
     
      """.trimIndent()
    )
    assertState(
      """
    foo
    foo
    bar
    
      """.trimIndent()
    )
  }

  // VIM-390 |yy| |p|
  fun testYankLinePasteAtLastLine() {
    typeTextInFile(
      "yy" + "p",
      """
     one two
     <caret>three four
     
      """.trimIndent()
    )
    assertState(
      """
    one two
    three four
    three four
    
      """.trimIndent()
    )
  }

  // |register| |y|
  fun testYankRegister() {
    typeTextInFile("\"ayl" + "l" + "\"byl" + "\"ap" + "\"bp", "hel<caret>lo world\n")
    assertState("hellolo world\n")
  }

  // |register| |y| |quote|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testYankRegisterUsesLastEnteredRegister() {
    typeTextInFile("\"a\"byl" + "\"ap", "hel<caret>lo world\n")
    assertState("helllo world\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testYankAppendRegister() {
    typeTextInFile("\"Ayl" + "l" + "\"Ayl" + "\"Ap", "hel<caret>lo world\n")
    assertState("hellolo world\n")
  }

  fun testYankWithInvalidRegister() {
    typeTextInFile("\"&", "hel<caret>lo world\n")
    assertPluginError(true)
  }

  // |P|
  fun testYankPutBefore() {
    typeTextInFile("y2l" + "P", "<caret>two\n")
    assertState("twtwo\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun testWrongYankQuoteMotion() {
    assertPluginError(false)
    typeTextInFile(
      "y\"",
      """
     one <caret>two
     three
     four
     
      """.trimIndent()
    )
    assertPluginError(true)
  }

  fun testWrongYankQuoteYankLine() {
    assertPluginError(false)
    typeTextInFile(
      "y\"" + "yy" + "p",
      """
     one <caret>two
     three
     four
     
      """.trimIndent()
    )
    assertPluginError(false)
    assertState(
      """
    one two
    one two
    three
    four
    
      """.trimIndent()
    )
  }

  fun testWrongYankRegisterMotion() {
    val editor = typeTextInFile(
      "y\"" + "0",
      """
     one <caret>two
     three
     four
     
      """.trimIndent()
    )
    assertEquals(0, editor.caretModel.offset)
  }

  // VIM-632 |CTRL-V| |v_y| |p|
  fun testYankVisualBlock() {
    typeTextInFile(
      "<C-V>" + "jl" + "yl" + "p",
      """
     <caret>* one
     * two
     
      """.trimIndent()
    )

    // XXX:
    // The correct output should be:
    //
    // * * one
    // * * two
    //
    // The problem is that the selection range should be 1-char wide when entering the visual block mode
    assertState(
      """
    * * one
    * * two
    
      """.trimIndent()
    )
    assertSelection(null)
    assertOffset(2)
  }

  // VIM-632 |CTRL-V| |v_y|
  fun testStateAfterYankVisualBlock() {
    typeTextInFile(
      "<C-V>" + "jl" + "y",
      """
     <caret>foo
     bar
     
      """.trimIndent()
    )
    assertOffset(0)
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSelection(null)
  }

  // VIM-476 |yy| |'clipboard'|
  // TODO: Review this test
  // This doesn't use the system clipboard, but the TestClipboardModel
  fun testClipboardUnnamed() {
    configureByText(
      """
    foo
    <caret>bar
    baz
    
      """.trimIndent()
    )
    assertEquals('\"', VimPlugin.getRegister().defaultRegister)
    enterCommand("set clipboard=unnamed")
    assertEquals('*', VimPlugin.getRegister().defaultRegister)
    typeText("yy")
    val starRegister = VimPlugin.getRegister().getRegister('*')
    assertNotNull(starRegister)
    assertEquals("bar\n", starRegister!!.text)
  }

  // VIM-792 |"*| |yy| |p|
  // TODO: Review this test
  // This doesn't use the system clipboard, but the TestClipboardModel
  fun testLineWiseClipboardYankPaste() {
    configureByText("<caret>foo\n")
    typeText("\"*yy" + "\"*p")
    val register = VimPlugin.getRegister().getRegister('*')
    assertNotNull(register)
    assertEquals("foo\n", register!!.text)
    assertState(
      """
    foo
    <caret>foo
    
      """.trimIndent()
    )
  }

  // VIM-792 |"*| |CTRL-V| |v_y| |p|
  // TODO: Review this test
  // This doesn't use the system clipboard, but the TestClipboardModel
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testBlockWiseClipboardYankPaste() {
    configureByText(
      """
    <caret>foo
    bar
    baz
    
      """.trimIndent()
    )
    typeText("<C-V>j" + "\"*y" + "\"*p")
    val register = VimPlugin.getRegister().getRegister('*')
    assertNotNull(register)
    assertEquals(
      """
    f
    b
      """.trimIndent(),
      register!!.text
    )
    assertState(
      """
    ffoo
    bbar
    baz
    
      """.trimIndent()
    )
  }

  // VIM-1431
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testPutInEmptyFile() {
    VimPlugin.getRegister().setKeys('a', injector.parser.parseKeys("test"))
    typeTextInFile("\"ap", "")
    assertState("test")
  }

  fun testOverridingRegisterWithEmptyTag() {
    configureByText(
      """
    <root>
    <a><caret>value</a>
    <b></b>
    </root>
    
      """.trimIndent()
    )
    typeText("dit", "j", "cit", "<C-R>\"")
    assertState(
      """
    <root>
    <a></a>
    <b>value</b>
    </root>
    
      """.trimIndent()
    )
  }
}

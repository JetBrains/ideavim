/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.vim
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
import org.jetbrains.plugins.ideavim.waitAndAssert

/**
 * @author vlan
 */
class MacroActionTest : VimTestCase() {
  // |q|
  fun testRecordMacro() {
    val editor = typeTextInFile(injector.parser.parseKeys("qa" + "3l" + "q"), "on<caret>e two three\n")
    val commandState = editor.vim.vimStateMachine
    assertFalse(commandState.isRecording)
    val registerGroup = VimPlugin.getRegister()
    val register = registerGroup.getRegister('a')
    assertNotNull(register)
    assertEquals("3l", register!!.text)
  }

  fun testRecordMacroDoesNotExpandMap() {
    configureByText("")
    enterCommand("imap pp hello")
    typeText(injector.parser.parseKeys("qa" + "i" + "pp<Esc>" + "q"))
    val register = VimPlugin.getRegister().getRegister('a')
    assertNotNull(register)
    assertEquals("ipp<Esc>", injector.parser.toKeyNotation(register!!.keys))
  }

  fun testRecordMacroWithDigraph() {
    typeTextInFile(injector.parser.parseKeys("qa" + "i" + "<C-K>OK<Esc>" + "q"), "")
    val register = VimPlugin.getRegister().getRegister('a')
    assertNotNull(register)
    assertEquals("i<C-K>OK<Esc>", injector.parser.toKeyNotation(register!!.keys))
  }

  fun `test macro with search`() {
    val content = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(content)
    typeText(injector.parser.parseKeys("qa" + "/rocks<CR>" + "q" + "gg" + "@a"))

    val startOffset = content.rangeOf("rocks").startOffset

    waitAndAssert {
      startOffset == myFixture.editor.caretModel.offset
    }
  }

  fun `test macro with command`() {
    val content = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(content)
    typeText(injector.parser.parseKeys("qa" + ":map x y<CR>" + "q"))

    val register = VimPlugin.getRegister().getRegister('a')
    val registerSize = register!!.keys.size
    TestCase.assertEquals(9, registerSize)
  }

  fun `test last command`() {
    val content = "${c}0\n1\n2\n3\n"
    configureByText(content)
    typeText(injector.parser.parseKeys(":d<CR>" + "@:"))
    assertState("2\n3\n")
  }

  fun `test last command with count`() {
    val content = "${c}0\n1\n2\n3\n4\n5\n"
    configureByText(content)
    typeText(injector.parser.parseKeys(":d<CR>" + "4@:"))
    assertState("5\n")
  }

  fun `test last command as last macro with count`() {
    val content = "${c}0\n1\n2\n3\n4\n5\n"
    configureByText(content)
    typeText(injector.parser.parseKeys(":d<CR>" + "@:" + "3@@"))
    assertState("5\n")
  }

  fun `test last command as last macro multiple times`() {
    val content = "${c}0\n1\n2\n3\n4\n5\n"
    configureByText(content)
    typeText(injector.parser.parseKeys(":d<CR>" + "@:" + "@@" + "@@"))
    assertState("4\n5\n")
  }

  // Broken, see the resulting text
  fun `ignore test macro with macro`() {
    val content = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(content)
    typeText(injector.parser.parseKeys("qa" + "l" + "q" + "qb" + "10@a" + "q" + "2@b"))

    val startOffset = content.rangeOf("rocks").startOffset

    waitAndAssert {
      println(myFixture.editor.caretModel.offset)
      println(startOffset)
      println()
      startOffset == myFixture.editor.caretModel.offset
    }
  }

  fun `test macro with count`() {
    configureByText("${c}0\n1\n2\n3\n4\n5\n")
    typeText(injector.parser.parseKeys("qajq" + "4@a"))
    assertState("0\n1\n2\n3\n4\n${c}5\n")
  }
}

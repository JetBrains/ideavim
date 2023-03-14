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
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * @author vlan
 */
class MacroActionTest : VimTestCase() {
  // |q|
  @Test
  fun testRecordMacro() {
    val editor = typeTextInFile(injector.parser.parseKeys("qa" + "3l" + "q"), "on<caret>e two three\n")
    val commandState = editor.vim.vimStateMachine
    kotlin.test.assertFalse(commandState.isRecording)
    val registerGroup = VimPlugin.getRegister()
    val register = registerGroup.getRegister('a')
    assertNotNull<Any>(register)
    kotlin.test.assertEquals("3l", register.text)
  }

  @Test
  fun testRecordMacroDoesNotExpandMap() {
    configureByText("")
    enterCommand("imap pp hello")
    typeText(injector.parser.parseKeys("qa" + "i" + "pp<Esc>" + "q"))
    val register = VimPlugin.getRegister().getRegister('a')
    assertNotNull<Any>(register)
    kotlin.test.assertEquals("ipp<Esc>", injector.parser.toKeyNotation(register.keys))
  }

  @Test
  fun testRecordMacroWithDigraph() {
    typeTextInFile(injector.parser.parseKeys("qa" + "i" + "<C-K>OK<Esc>" + "q"), "")
    val register = VimPlugin.getRegister().getRegister('a')
    assertNotNull<Any>(register)
    kotlin.test.assertEquals("i<C-K>OK<Esc>", injector.parser.toKeyNotation(register.keys))
  }

  @Test
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
      startOffset == fixture.editor.caretModel.offset
    }
  }

  @Test
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
    kotlin.test.assertEquals(9, registerSize)
  }

  @Test
  fun `test last command`() {
    val content = "${c}0\n1\n2\n3\n"
    configureByText(content)
    typeText(injector.parser.parseKeys(":d<CR>" + "@:"))
    assertState("2\n3\n")
  }

  @Test
  fun `test last command with count`() {
    val content = "${c}0\n1\n2\n3\n4\n5\n"
    configureByText(content)
    typeText(injector.parser.parseKeys(":d<CR>" + "4@:"))
    assertState("5\n")
  }

  @Test
  fun `test last command as last macro with count`() {
    val content = "${c}0\n1\n2\n3\n4\n5\n"
    configureByText(content)
    typeText(injector.parser.parseKeys(":d<CR>" + "@:" + "3@@"))
    assertState("5\n")
  }

  @Test
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
      println(fixture.editor.caretModel.offset)
      println(startOffset)
      println()
      startOffset == fixture.editor.caretModel.offset
    }
  }

  @Test
  fun `test macro with count`() {
    configureByText("${c}0\n1\n2\n3\n4\n5\n")
    typeText(injector.parser.parseKeys("qajq" + "4@a"))
    assertState("0\n1\n2\n3\n4\n${c}5\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT, "Reports differences in 'a register")
  @Test
  fun `test stop on error`() {
    val content = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(content)
    typeText(injector.parser.parseKeys("qa" + "i1<esc>j" + "q" + "gg" + "10@a"))

    assertState(
      """
            1A Discovery
            1
            11I found it in a legendary land
            1all rocks and lavender and tufted grass,
            1where it was settled on some sodden sand
            ${c}1hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }
}

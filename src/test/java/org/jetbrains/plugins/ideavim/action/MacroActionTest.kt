/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.intellij.idea.TestFor
import com.intellij.testFramework.LoggedErrorProcessor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.keys
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.ExceptionHandler
import org.jetbrains.plugins.ideavim.OnlyThrowLoggedErrorProcessor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.exceptionMappingOwner
import org.jetbrains.plugins.ideavim.rangeOf
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @author vlan
 */
class MacroActionTest : VimTestCase() {

  @AfterEach
  fun tearDown() {
    injector.keyGroup.removeKeyMapping(exceptionMappingOwner)
  }

  // |q|
  @Test
  fun testRecordMacro() {
    typeTextInFile(injector.parser.parseKeys("qa" + "3l" + "q"), "on<caret>e two three\n")
    kotlin.test.assertFalse(injector.registerGroup.isRecording)
    assertRegister('a', "3l")
  }

  @Test
  fun testRecordMacroDoesNotExpandMap() {
    configureByText("")
    enterCommand("imap pp hello")
    typeText(injector.parser.parseKeys("qa" + "i" + "pp<Esc>" + "q"))
    assertRegister('a', "ipp^[")
  }

  @Test
  fun testRecordMacroWithDigraph() {
    typeTextInFile(injector.parser.parseKeys("qa" + "i" + "<C-K>OK<Esc>" + "q"), "")
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val register = registerService.getRegister(vimEditor, context, 'a')
    assertNotNull<Any>(register)
    assertRegister('a', "i^KOK^[")
  }

  @Test
  fun `test macro with search`() {
    val content = """
            Lorem Ipsum

            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(content)
    typeText(injector.parser.parseKeys("qa" + "/consectetur<CR>" + "q" + "gg" + "@a"))

    val startOffset = content.rangeOf("consectetur").startOffset

    waitAndAssert {
      startOffset == fixture.editor.caretModel.offset
    }
  }

  @Test
  fun `test macro with command`() {
    val content = """
            Lorem Ipsum

            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(content)
    typeText(injector.parser.parseKeys("qa" + ":map x y<CR>" + "q"))

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val register = registerService.getRegister(vimEditor, context, 'a')
    val registerSize = register!!.keys.size
    assertEquals(9, registerSize)
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

  @Test
  fun `test macro with macro`() {
    val content = """
            Lorem Ipsum

            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(content)
    typeText(
      injector.parser.parseKeys(
        "qa" + "l" + "q" +
          "qb" + "6@a" + "q" +
          "^" + "3@b"
      )
    )

    assertRegister('b', "6@a")
    assertState(
      """
            Lorem Ipsum

            Lorem ipsum dolor ${c}sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )
  }

  @Test
  fun `test macro with macro with macro`() {
    val content = """
            Lorem Ipsum

            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(content)
    typeText(
      injector.parser.parseKeys(
        "qa" + "l" + "q" +
          "qb" + "3@a" + "q" +
          "qc" + "2@b" + "q" +
          "^" + "3@c"
      )
    )

    assertRegister('b', "3@a")
    assertRegister('c', "2@b")
    assertState(
      """
            Lorem Ipsum

            Lorem ipsum dolor ${c}sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )
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
            Lorem Ipsum

            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(content)
    typeText(injector.parser.parseKeys("qa" + "i1<esc>j" + "q" + "gg" + "10@a"))

    assertState(
      """
            1Lorem Ipsum
            1
            11Lorem ipsum dolor sit amet,
            1consectetur adipiscing elit
            1Sed in orci mauris.
            ${c}1Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }

  @TestFor(issues = ["VIM-2929"])
  @TestWithoutNeovim(reason = SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `macro to handler with exception`() {
    configureByText(
      """
     Lorem Ipsum

     Lorem ipsum dolor sit amet,
     ${c}consectetur adipiscing elit
     Sed in orci mauris.
     Cras id tellus in ex imperdiet egestas. 
    """.trimIndent()
    )
    injector.keyGroup.putKeyMapping(MappingMode.NXO, keys("abc"), exceptionMappingOwner, ExceptionHandler(), false)

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, 'k', "abc")
    injector.registerGroup.storeText(vimEditor, context, 'q', "x@ky")

    val exception = assertThrows<Throwable> {
      LoggedErrorProcessor.executeWith<Throwable>(OnlyThrowLoggedErrorProcessor) {
        typeText("@q")
      }
    }
    assertEquals(ExceptionHandler.exceptionMessage, exception.cause!!.cause!!.message)

    assertTrue(KeyHandler.getInstance().keyStack.isEmpty())
  }
}

/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class YankMotionActionTest : VimTestCase() {
  @Test
  fun `test yank till new line`() {
    val file = """
            Lorem Ipsum

            I found it in a legendary l${c}and
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    typeTextInFile("yW", file)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val text =
      registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text ?: kotlin.test.fail()

    kotlin.test.assertEquals("and", text)
  }

  @Test
  fun `test yank caret doesn't move`() {
    val file = """
            Lorem Ipsum

            I found it in a legendary l${c}and
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(file)

    val initialOffset = fixture.editor.caretModel
    typeText("yy")

    kotlin.test.assertEquals(initialOffset, fixture.editor.caretModel)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  @Test
  fun `test unnamed saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    enterCommand("set clipboard=unnamed")
    typeText("yiw")

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val starRegister = registerService.getRegister(vimEditor, context, '*') ?: kotlin.test.fail("Register * is empty")
    kotlin.test.assertEquals("legendary", starRegister.text)

    val quoteRegister = registerService.getRegister(vimEditor, context, '"') ?: kotlin.test.fail("Register \" is empty")
    kotlin.test.assertEquals("legendary", quoteRegister.text)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  @Test
  fun `test z saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText("\"zyiw")

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val starRegister = registerService.getRegister(vimEditor, context, 'z') ?: kotlin.test.fail("Register z is empty")
    kotlin.test.assertEquals("legendary", starRegister.text)

    val quoteRegister = registerService.getRegister(vimEditor, context, '"') ?: kotlin.test.fail("Register \" is empty")
    kotlin.test.assertEquals("legendary", quoteRegister.text)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  @Test
  fun `test " saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText("\"zyiw")

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val quoteRegister = registerService.getRegister(vimEditor, context, '"') ?: kotlin.test.fail("Register \" is empty")
    kotlin.test.assertEquals("legendary", quoteRegister.text)
  }

  @Test
  fun `test yank up`() {
    val file = """
            A ${c}Discovery

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    typeTextInFile("yk", file)

    kotlin.test.assertTrue(injector.messages.isError())
  }

  @Test
  fun `test yank dollar at last empty line`() {
    val file = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
            $c
    """.trimIndent()
    typeTextInFile("y$", file)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val text =
      registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text ?: kotlin.test.fail()

    kotlin.test.assertEquals("", text)
  }

  @Test
  fun `test yank to star with mapping`() {
    val file = """
            Lorem Ipsum

            I found it in a ${c}legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    typeTextInFile(commandToKeys("map * *zz"), file)
    typeTextInFile("\"*yiw", file)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val text =
      registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text ?: kotlin.test.fail()

    kotlin.test.assertEquals("legendary", text)
  }

  @Test
  fun `test yank to star with yank mapping`() {
    val file = """
            Lorem Ipsum

            I found it in a ${c}legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    typeTextInFile(commandToKeys("map * *yiw"), file)
    typeTextInFile("\"*", file)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    kotlin.test.assertNull(registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text)
  }

  @Test
  fun `test yank last line`() {
    val file = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent$c of a mountain pass.
    """.trimIndent()

    doTest("yy", file, file)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val text =
      registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text ?: kotlin.test.fail()

    kotlin.test.assertEquals("hard by the torrent of a mountain pass.\n", text)
  }

  @Test
  fun `test yank block works linewise if at start of line`() {
    val file = """
            Lorem Ipsum

               ${c}dolor sit amet

            I found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    typeTextInFile("y}", file)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val register = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)
    val text = register?.text ?: kotlin.test.fail()
    val type = register.type

    kotlin.test.assertEquals("dolor sit amet\n", text)
    kotlin.test.assertEquals(SelectionType.LINE_WISE, type)
  }

  @Test
  fun `test yank block works linewise mixing linewise and characterwise motion`() {
    val file = """
            Lorem Ipsum

               ${c}dolor sit amet

            I found it in a legendary land
            
            consectetur ${c}adipiscing elit
            
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    typeTextInFile("y}", file)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    val carets = injector.application.runReadAction {
      vimEditor.carets().sortedBy { it.offset }
    }

    assertEquals(2, carets.size)

    val firstRegister = carets.first().registerStorage.getRegister(vimEditor, context, registerService.lastRegisterChar)
    assertEquals("dolor sit amet\n", firstRegister!!.text)
    assertEquals(SelectionType.LINE_WISE, firstRegister.type)

    val secondRegister = carets.last().registerStorage.getRegister(vimEditor, context, registerService.lastRegisterChar)
    assertEquals("adipiscing elit\n", secondRegister!!.text)
    assertEquals(SelectionType.CHARACTER_WISE, secondRegister.type)
  }

  @Test
  fun `test yank block works charwise if not at start of line`() {
    val file = """
            Lorem Ipsum

               d${c}olor sit amet

            I found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    typeTextInFile("y}", file)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val register = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)
    val text = register?.text ?: kotlin.test.fail()
    val type = register.type

    kotlin.test.assertEquals("olor sit amet\n", text)
    kotlin.test.assertEquals(SelectionType.CHARACTER_WISE, type)
  }
}

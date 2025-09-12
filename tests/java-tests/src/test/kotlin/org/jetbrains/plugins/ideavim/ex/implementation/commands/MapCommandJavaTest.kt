/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.jetbrains.plugins.ideavim.assertThrowsLogError
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.junit.jupiter.api.Test
import javax.swing.JTextArea
import kotlin.test.assertIs

class MapCommandJavaTest : VimJavaTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test comment line with action`() {
    configureByJavaText(
      """
        -----
        1<caret>2345
        abcde
        -----
      """.trimIndent(),
    )
    typeText(commandToKeys("map k <Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("k"))
    assertState(
      """
        -----
        //12345
        abcde
        -----
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test execute two actions with two mappings`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent(),
    )
    typeText(commandToKeys("map k <Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("kk"))
    assertState(
      """
          -----
          //12345
          //abcde
          -----
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test execute two actions with single mappings`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent(),
    )
    typeText(commandToKeys("map k <Action>(CommentByLineComment)<Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("k"))
    assertState(
      """
          -----
          //12345
          //abcde
          -----
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test execute three actions with single mappings`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent(),
    )
    typeText(commandToKeys("map k <Action>(CommentByLineComment)<Action>(CommentByLineComment)<Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("k"))
    assertState(
      """
          -----
          //12345
          //abcde
          //-----
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test execute action from insert mode`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent(),
    )
    typeText(commandToKeys("imap k <Action>(CommentByLineComment)"))
    typeText(injector.parser.parseKeys("ik"))
    assertState(
      """
          -----
          //12345
          abcde
          -----
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `action execution has correct ordering`() {
    configureByJavaText(
      """
          -----
          1<caret>2345
          abcde
          -----
      """.trimIndent(),
    )
    typeText(commandToKeys("map k <Action>(EditorDown)x"))
    typeText(injector.parser.parseKeys("k"))
    assertState(
      """
          -----
          12345
          a${c}cde
          -----
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test execute mapping with a delay`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map kk l"))
    typeText(injector.parser.parseKeys("k"))

    ApplicationManager.getApplication().invokeAndWait {
      checkDelayedMapping(
        text,
        """
              -$c----
              12345
              abcde
              -----
      """.trimIndent(),
      )
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun `test execute mapping with a delay and second mapping`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map k j"))
    typeText(commandToKeys("map kk l"))
    typeText(injector.parser.parseKeys("k"))

    ApplicationManager.getApplication().invokeAndWait {
      checkDelayedMapping(
        text,
        """
              -----
              12345
              a${c}bcde
              -----
      """.trimIndent(),
      )
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test execute mapping with a delay and second mapping and another starting mappings`() {
    // TODO: 24.01.2021  mapping time should be only 1000 sec
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map k j"))
    typeText(commandToKeys("map kk l"))
    typeText(commandToKeys("map j h"))
    typeText(commandToKeys("map jz w"))
    typeText(injector.parser.parseKeys("k"))

    ApplicationManager.getApplication().invokeAndWait {
      checkDelayedMapping(
        text,
        """
              -----
              ${c}12345
              abcde
              -----
      """.trimIndent(),
      )
    }
  }

  @Test
  fun `test execute mapping with a delay and second mapping and another starting mappings with another key`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map k j"))
    typeText(commandToKeys("map kk l"))
    typeText(commandToKeys("map j h"))
    typeText(commandToKeys("map jz w"))
    typeText(injector.parser.parseKeys("kz"))

    assertState(
      """
              -----
              12345
              ${c}abcde
              -----
      """.trimIndent(),
    )
  }

  @Test
  fun `test recursion`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map x y"))
    typeText(commandToKeys("map y x"))
    typeText(injector.parser.parseKeys("x"))

    kotlin.test.assertTrue(injector.messages.isError())
  }

  @Test
  fun `test map with expression`() {
    // we test that ternary expression works and the cursor stays at the same place after leaving normal mode
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)
    typeText(commandToKeys("inoremap <expr> jk col(\".\") == 1? '<Esc>' : '<Esc><Right>'"))
    typeText(injector.parser.parseKeys("ijk"))
    assertState(text)
    val text2 = """
          -----
          ${c}12345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text2)
    typeText(injector.parser.parseKeys("ijk"))
    assertState(text2)
  }

  @Test
  fun `test map with invalid expression`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)
    typeText(commandToKeys("nnoremap <expr> t ^f8a"))
    typeText(injector.parser.parseKeys("t"))
    assertPluginErrorMessage("E15: Invalid expression: ^f8a")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test map expr context`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    val editor = TextComponentEditorImpl(null, JTextArea())
    val context = DataContext.EMPTY_CONTEXT
    injector.vimscriptExecutor.execute(
      """
      let s:mapping = '^f8a'
      nnoremap <expr> t s:mapping
      """.trimIndent(),
      editor.vim,
      context.vim,
      skipHistory = false,
      indicateErrors = true,
      null,
    )
    val exception = assertThrowsLogError<Throwable> {
      typeText(injector.parser.parseKeys("t"))
    }
    assertIs<ExException>(exception.cause!!.cause) // Exception is wrapped into LOG.error twice

    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: s:mapping")
    editor.caretModel.allCarets.forEach { Disposer.dispose(it) }
  }

  // Related issue: VIM-2315
  @Test
  fun `test with shorter conflict`() {
    val text = """
          -----
          1${c}2345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    typeText(commandToKeys("map kkk l"))
    typeText(commandToKeys("map kk h"))
    typeText(injector.parser.parseKeys("kk"))

    ApplicationManager.getApplication().invokeAndWait {
      checkDelayedMapping(
        text,
        """
              -----
              ${c}12345
              abcde
              -----
      """.trimIndent(),
      )
    }
    assertMode(Mode.NORMAL())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test exception during expression evaluation in map with expression`() {
    val text = """
          -----
          ${c}12345
          abcde
          -----
    """.trimIndent()
    configureByJavaText(text)

    val exception = assertThrowsLogError<Throwable> {
      typeText(commandToKeys("inoremap <expr> <cr> unknownFunction() ? '\\<C-y>' : '\\<C-g>u\\<CR>'"))
      typeText(injector.parser.parseKeys("i<CR>"))
    }
    assertIs<ExException>(exception.cause)

    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: unknownFunction")
    assertState(text)
  }

  private fun checkDelayedMapping(before: String, after: String) {
    assertState(before)

    waitAndAssert(5000) {
      return@waitAndAssert try {
        assertState(after)
        true
      } catch (e: AssertionError) {
        false
      }
    }
  }
}

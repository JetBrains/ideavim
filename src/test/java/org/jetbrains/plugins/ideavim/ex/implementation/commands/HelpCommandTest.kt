/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.TextRange
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.CmdwinKeys
import com.maddyhome.idea.vim.vimscript.model.commands.HelpCommand
import com.maddyhome.idea.vim.vimscript.model.commands.HelpFile
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HelpCommandTest : VimTestCase() {
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("help :help")
    assertTrue(command is HelpCommand)
    assertEquals(":help", command.argument)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "IdeaVim has its own help file")
  @Test
  fun `help without a subject opens the start of the help`() {
    configureByText("${c}text\n")
    enterCommand("help")

    val helpEditor = assertNotNull(helpEditor())
    assertEquals(0, runReadAction { helpEditor.caretModel.logicalPosition.line })
    assertTrue(helpEditor.document.text.startsWith("*ideavim.txt*"))
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "IdeaVim has its own help file")
  @Test
  fun `help jumps to the tag of an ex command`() {
    configureByText("${c}text\n")
    enterCommand("help :s")

    assertCaretOnTag(":s")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "IdeaVim has its own help file")
  @Test
  fun `help jumps to the tag of a mode command`() {
    configureByText("${c}text\n")
    enterCommand("help i_CTRL-W")

    assertCaretOnTag("i_CTRL-W")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "IdeaVim has its own help file")
  @Test
  fun `the help is read-only`() {
    configureByText("${c}text\n")
    enterCommand("help")

    val helpEditor = assertNotNull(helpEditor())
    assertFalse(helpEditor.document.isWritable)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT, "IdeaVim has its own help file")
  @Test
  fun `an open help is reused`() {
    configureByText("${c}text\n")
    enterCommand("help :s")
    enterCommand("help :help")

    val helpFiles = FileEditorManager.getInstance(fixture.project).openFiles
      .filter { it.getUserData(CmdwinKeys.KIND) == VirtualBufferKind.Help }
    assertEquals(1, helpFiles.size)
    assertCaretOnTag(":help")
  }

  @Test
  fun `unknown subject reports an error`() {
    configureByText("${c}text\n")
    enterCommand("help nosuchsubjectanywhere")

    assertPluginError(true)
    assertPluginErrorMessage("E149: Sorry, no help for nosuchsubjectanywhere")
    assertNull(helpEditor())
  }

  @Test
  fun `subject is an exact tag`() {
    assertTagLine("v_d", HelpFile.findTag("v_d"))
    assertTagLine(":help", HelpFile.findTag(":help"))
  }

  @Test
  fun `subject without a colon finds the ex command`() {
    assertTagLine(":help", HelpFile.findTag("help"))
  }

  @Test
  fun `subject in mapping notation`() {
    assertTagLine("CTRL-W_h", HelpFile.findTag("<C-W>h"))
    assertTagLine("i_CTRL-W", HelpFile.findTag("i_<C-W>"))
  }

  @Test
  fun `subject in caret notation`() {
    assertTagLine("CTRL-W_h", HelpFile.findTag("^Wh"))
    assertTagLine("g^", HelpFile.findTag("g^"))
  }

  @Test
  fun `subject with a character that delimits tags`() {
    assertTagLine("star", HelpFile.findTag("*"))
    assertTagLine("gstar", HelpFile.findTag("g*"))
  }

  @Test
  fun `doubled operator finds the operator`() {
    assertTagLine("d", HelpFile.findTag("dd"))
    assertTagLine("gU", HelpFile.findTag("gUU"))
  }

  @Test
  fun `subject is the start of a tag`() {
    assertTagLine(":substitute", HelpFile.findTag(":substitut"))
  }

  @Test
  fun `unknown subject has no tag`() {
    assertNull(HelpFile.findTag("nosuchsubjectanywhere"))
  }

  private fun helpEditor(): Editor? {
    val fileEditorManager = FileEditorManager.getInstance(fixture.project)
    val file = fileEditorManager.openFiles.firstOrNull { it.getUserData(CmdwinKeys.KIND) == VirtualBufferKind.Help }
      ?: return null
    return fileEditorManager.getEditors(file).filterIsInstance<TextEditor>().firstOrNull()?.editor
  }

  private fun assertCaretOnTag(tag: String) {
    val helpEditor = assertNotNull(helpEditor())
    val text = runReadAction {
      val document = helpEditor.document
      val line = document.getLineNumber(helpEditor.caretModel.offset)
      document.getText(TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)))
    }
    assertTrue(text.contains("*$tag*"), "Expected the caret on the line of *$tag*, but it is on '$text'")
  }

  private fun assertTagLine(tag: String, line: Int?) {
    assertNotNull(line, "Expected a line for *$tag*")
    val text = HelpFile.text.lines()[line]
    assertTrue(text.contains("*$tag*"), "Expected *$tag* on line $line, but it is '$text'")
  }
}

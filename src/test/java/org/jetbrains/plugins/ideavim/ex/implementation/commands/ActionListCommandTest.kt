/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.maddyhome.idea.vim.vimscript.model.commands.ActionListBuffer
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.CmdwinKeys
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("SpellCheckingInspection")
class ActionListCommandTest : VimTestCase() {
  @Test
  fun testListAllActions() {
    configureByText("\n")
    typeText(commandToKeys("actionlist"))
    val displayedLines = parseActionListOutput()

    assertEquals("--- Actions ---", displayedLines[0])
    val actionIds = ActionManager.getInstance().getActionIdList("")
    assertEquals(actionIds.size, displayedLines.size - 1)
  }

  @Test
  fun testSearchByActionName() {
    configureByText("\n")
    typeText(commandToKeys("actionlist quickimpl"))
    val displayedLines = parseActionListOutput()

    assertEquals("--- Actions ---", displayedLines[0])
    assertTrue(displayedLines.size > 1)
    displayedLines.drop(1).forEach { assertTrue(it.lowercase(Locale.getDefault()).contains("quickimpl")) }
  }

  @Test
  fun testSearchByAssignedShortcutKey() {
    configureByText("\n")
    typeText(commandToKeys("actionlist <M-S-"))
    val displayedLines = parseActionListOutput()

    assertEquals("--- Actions ---", displayedLines[0])
    displayedLines.drop(1).forEach { assertTrue(it.lowercase(Locale.getDefault()).contains("<m-s-")) }
  }

  @Test
  fun `action list opens in a read-only buffer instead of the output panel`() {
    configureByText("\n")
    typeText(commandToKeys("actionlist quickimpl"))

    val actionListEditor = assertNotNull(actionListEditor())
    assertFalse(actionListEditor.document.isWritable)
    assertEquals(0, runReadAction { actionListEditor.caretModel.offset })
    assertNull(injector.outputPanel.getCurrentOutputPanel())
  }

  @Test
  fun `an open action list is reused with the new search result`() {
    configureByText("\n")
    typeText(commandToKeys("actionlist quickimpl"))
    typeText(commandToKeys("actionlist reformatcode"))

    val actionListFiles = FileEditorManager.getInstance(fixture.project).openFiles
      .filter { it.getUserData(CmdwinKeys.KIND) == ActionListBuffer }
    assertEquals(1, actionListFiles.size)
    val displayedLines = parseActionListOutput()
    assertTrue(displayedLines.size > 1)
    displayedLines.drop(1).forEach { assertTrue(it.lowercase(Locale.getDefault()).contains("reformatcode")) }
  }

  @Test
  fun `enter executes the action on the line in the original editor`() {
    configureByText("${c}first\nsecond\n")
    val originalEditor = fixture.editor
    typeText(commandToKeys("actionlist EditorDown"))
    val line = parseActionListOutput().indexOfFirst { it.substringBefore(' ') == "EditorDown" }
    assertTrue(line > 0)

    typeInActionList("${line + 1}G<CR>")

    assertEquals(1, runReadAction { originalEditor.caretModel.logicalPosition.line })
  }

  @Test
  fun `enter on the header does nothing`() {
    configureByText("${c}first\nsecond\n")
    val originalEditor = fixture.editor
    typeText(commandToKeys("actionlist EditorDown"))

    typeInActionList("gg<CR>")

    assertEquals(0, runReadAction { originalEditor.caretModel.logicalPosition.line })
    assertEquals(0, runReadAction { assertNotNull(actionListEditor()).caretModel.offset })
  }

  private fun typeInActionList(keys: String) {
    val editor = assertNotNull(actionListEditor())
    ApplicationManager.getApplication().invokeAndWait {
      VimTestCase.typeText(injector.parser.parseKeys(keys), editor, fixture.project)
    }
  }

  private fun actionListEditor(): Editor? {
    val fileEditorManager = FileEditorManager.getInstance(fixture.project)
    val file = fileEditorManager.openFiles
      .firstOrNull { it.getUserData(CmdwinKeys.KIND) == ActionListBuffer }
      ?: return null
    return fileEditorManager.getEditors(file).filterIsInstance<TextEditor>().firstOrNull()?.editor
  }

  private fun parseActionListOutput(): List<String> {
    val editor = assertNotNull(actionListEditor())
    return runReadAction { editor.document.text }.lines().dropLastWhile { it.isEmpty() }
  }
}

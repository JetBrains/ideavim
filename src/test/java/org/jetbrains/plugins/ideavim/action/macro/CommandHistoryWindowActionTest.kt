/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.macro

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.api.HistoryWindowKind
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.helper.CmdwinKeys
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommandHistoryWindowActionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    injector.historyGroup.resetHistory()
    configureByText("hello world\n")
  }

  @Test
  fun `q colon opens command history window`() {
    enterCommand("set digraph")
    enterCommand("set incsearch")

    typeText("q:")

    val cmdwin = openedCmdwin()
    assertEquals(HistoryWindowKind.Command, cmdwin.getUserData(CmdwinKeys.KIND))
    assertEquals("set digraph\nset incsearch", cmdwin.readContent())
    assertFalse(injector.registerGroup.isRecording)
  }

  @Test
  fun `q slash opens forward search history window`() {
    enterSearch("alpha")
    enterSearch("bravo")

    typeText("q/")

    val cmdwin = openedCmdwin()
    assertEquals(HistoryWindowKind.Search(Direction.FORWARDS), cmdwin.getUserData(CmdwinKeys.KIND))
    assertEquals("alpha\nbravo", cmdwin.readContent())
  }

  @Test
  fun `q question opens backward search history window`() {
    enterSearch("alpha")

    typeText("q?")

    val cmdwin = openedCmdwin()
    assertEquals(HistoryWindowKind.Search(Direction.BACKWARDS), cmdwin.getUserData(CmdwinKeys.KIND))
    assertEquals("alpha", cmdwin.readContent())
  }

  @Test
  fun `q colon with empty history opens an empty window`() {
    typeText("q:")
    assertEquals("", openedCmdwin().readContent())
  }

  @Test
  fun `cmdwin editor reports its history window kind via the engine API`() {
    typeText("q:")
    val cmdwin = openedCmdwin()
    var kind: HistoryWindowKind? = null
    ApplicationManager.getApplication().invokeAndWait {
      kind = openTextEditor(cmdwin).vim.getHistoryWindowKind()
    }
    assertEquals(HistoryWindowKind.Command, kind)
  }

  @Test
  fun `enter on a cmdwin line executes the command against the original editor and closes the cmdwin`() {
    enterCommand("set nodigraph")
    enterCommand("set digraph") // digraph option is currently ON

    typeText("q:")
    val cmdwin = openedCmdwin()

    ApplicationManager.getApplication().invokeAndWait {
      val editor = openTextEditor(cmdwin)
      editor.caretModel.moveToOffset(0) // first line: "set nodigraph"
      VimTestCase.typeText(injector.parser.parseKeys("<CR>"), editor, fixture.project)
    }

    assertFalse(
      FileEditorManager.getInstance(fixture.project).openFiles.any { it.name == "[Command Line]" },
      "cmdwin should be closed after <CR>",
    )
    assertFalse(
      injector.optionGroup.getOptionValue(
        injector.optionGroup.getOption("digraph")!!,
        com.maddyhome.idea.vim.options.OptionAccessScope.EFFECTIVE(fixture.editor.vim),
      ).toVimNumber().booleanValue,
      "set nodigraph from the cmdwin should have been executed",
    )
  }

  @Test
  fun `enter on a search cmdwin line jumps the caret to the match in the original editor`() {
    configureByText("alpha bravo charlie\n")
    enterSearch("charlie")

    typeText("q/")
    val cmdwin = openedCmdwin()

    ApplicationManager.getApplication().invokeAndWait {
      val editor = openTextEditor(cmdwin)
      editor.caretModel.moveToOffset(0) // line: "charlie"
      VimTestCase.typeText(injector.parser.parseKeys("<CR>"), editor, fixture.project)
    }

    val charliePos = fixture.editor.document.text.indexOf("charlie")
    var caretOffset = -1
    ApplicationManager.getApplication().invokeAndWait {
      caretOffset = fixture.editor.caretModel.offset
    }
    assertEquals(charliePos, caretOffset, "caret should jump to the search hit")
    assertFalse(
      FileEditorManager.getInstance(fixture.project).openFiles.any {
        it.getUserData(CmdwinKeys.KIND) != null
      },
      "cmdwin should close after <CR>",
    )
  }

  @Test
  fun `enter on a blank cmdwin line still closes the cmdwin`() {
    typeText("q:") // no command history -> cmdwin contains a single empty line
    val cmdwin = openedCmdwin()

    ApplicationManager.getApplication().invokeAndWait {
      val editor = openTextEditor(cmdwin)
      editor.caretModel.moveToOffset(0)
      VimTestCase.typeText(injector.parser.parseKeys("<CR>"), editor, fixture.project)
    }

    assertFalse(
      FileEditorManager.getInstance(fixture.project).openFiles.any {
        it.getUserData(CmdwinKeys.KIND) != null
      },
      "cmdwin should close on <CR> even when the line is blank",
    )
  }

  @Test
  fun `q colon while cmdwin already open is refused with an error`() {
    typeText("q:") // open the first cmdwin

    typeText("q:") // attempt to open another

    assertEquals(1, openCmdwinCount(), "should not have opened a second cmdwin")
    assertEquals(
      "E1292: Command-line window is already open",
      injector.messages.getStatusBarMessage(),
    )
  }

  @Test
  fun `q slash while cmdwin already open is refused with an error`() {
    typeText("q:")

    typeText("q/")

    assertEquals(1, openCmdwinCount())
    assertEquals("E1292: Command-line window is already open", injector.messages.getStatusBarMessage())
  }

  @Test
  fun `q question while cmdwin already open is refused with an error`() {
    typeText("q/")

    typeText("q?")

    assertEquals(1, openCmdwinCount())
    assertEquals("E1292: Command-line window is already open", injector.messages.getStatusBarMessage())
  }

  @Test
  fun `qa still starts macro recording`() {
    typeText("qa")
    assertTrue(injector.registerGroup.isRecording)
    typeText("q")
    assertFalse(injector.registerGroup.isRecording)
  }

  private fun openCmdwinCount(): Int =
    FileEditorManager.getInstance(fixture.project).openFiles
      .count { it.getUserData(CmdwinKeys.KIND) != null }

  private fun openedCmdwin(): VirtualFile {
    val files = FileEditorManager.getInstance(fixture.project).openFiles
    val match = files.firstOrNull { it.getUserData(CmdwinKeys.KIND) != null }
    assertNotNull(match, "expected a cmdwin VirtualFile in open editors, got ${files.map { it.name }}")
    return match
  }

  private fun openTextEditor(file: VirtualFile) =
    (FileEditorManager.getInstance(fixture.project).openFile(file, true).first() as TextEditor).editor

  private fun VirtualFile.readContent(): String = String(contentsToByteArray(), charset)
}

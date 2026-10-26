/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.macro

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.helper.CmdwinKeys
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    assertEquals(VirtualBufferKind.Command, cmdwin.getUserData(CmdwinKeys.KIND))
    assertEquals("set digraph\nset incsearch", cmdwin.readContent())
    assertFalse(injector.registerGroup.isRecording)
  }

  @Test
  fun `q slash opens forward search history window`() {
    enterSearch("alpha")
    enterSearch("bravo")

    typeText("q/")

    val cmdwin = openedCmdwin()
    assertEquals(VirtualBufferKind.Search(Direction.FORWARDS), cmdwin.getUserData(CmdwinKeys.KIND))
    assertEquals("alpha\nbravo", cmdwin.readContent())
  }

  @Test
  fun `q question opens backward search history window`() {
    enterSearch("alpha")

    typeText("q?")

    val cmdwin = openedCmdwin()
    assertEquals(VirtualBufferKind.Search(Direction.BACKWARDS), cmdwin.getUserData(CmdwinKeys.KIND))
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
    var kind: VirtualBufferKind? = null
    ApplicationManager.getApplication().invokeAndWait {
      kind = openTextEditor(cmdwin).vim.getVirtualBufferKind()
    }
    assertEquals(VirtualBufferKind.Command, kind)
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
    assertFalse(isDigraphEnabled(), "set nodigraph from the cmdwin should have been executed")
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

  // The cmdwin editor was not recognised while it was being created (its virtual file is not attached to the editor
  // yet at that point), so it was treated as a non-Vim editor and never had its local options initialised. Search
  // count and search highlights are computed for every open editor, so searching for a pattern with `\<` asked the
  // cmdwin buffer for its local 'iskeyword' and threw "Unexpected uninitialised buffer local value: iskeyword"
  @Test
  fun `search with a word boundary pattern while the cmdwin is open does not crash`() {
    configureByText("${c}alpha bravo charlie\n")
    enterSearch("alpha") // give the search history a line, so `q?` has something to show

    typeText("q?")
    openedCmdwin() // the cmdwin is now the selected editor, and search count is computed against it

    // Search in the original editor while the cmdwin split is still open
    ApplicationManager.getApplication().invokeAndWait {
      VimTestCase.typeText(injector.parser.parseKeys("/\\<bravo<CR>"), fixture.editor, fixture.project)
    }

    assertOffset("alpha bravo charlie".indexOf("bravo"))
  }

  @Test
  fun `ctrl-f in command line opens command history window with the typed text as the last line`() {
    enterCommand("set nodigraph")

    typeText(":set dig<C-F>")

    val cmdwin = openedCmdwin()
    assertEquals(VirtualBufferKind.Command, cmdwin.getUserData(CmdwinKeys.KIND))
    assertEquals("set nodigraph\nset dig", cmdwin.readContent())
  }

  @Test
  fun `ctrl-f in forward search opens forward search history window with the typed pattern as the last line`() {
    enterSearch("alpha")

    typeText("/bra<C-F>")

    val cmdwin = openedCmdwin()
    assertEquals(VirtualBufferKind.Search(Direction.FORWARDS), cmdwin.getUserData(CmdwinKeys.KIND))
    assertEquals("alpha\nbra", cmdwin.readContent())
  }

  @Test
  fun `ctrl-f in backward search opens backward search history window`() {
    typeText("?bra<C-F>")

    val cmdwin = openedCmdwin()
    assertEquals(VirtualBufferKind.Search(Direction.BACKWARDS), cmdwin.getUserData(CmdwinKeys.KIND))
    assertEquals("bra", cmdwin.readContent())
  }

  @Test
  fun `ctrl-f closes the command line and does not add the typed text to history`() {
    typeText(":set dig<C-F>")

    openedCmdwin()
    assertNull(injector.commandLine.getActiveCommandLine())
    assertTrue(injector.historyGroup.getEntries(VimHistory.Type.Command, 0, 0).isEmpty())
  }

  @Test
  fun `ctrl-f in an expression prompt does not open a cmdwin`() {
    typeText("i<C-R>=1+1<C-F>")

    assertEquals(0, openCmdwinCount())
    assertNull(injector.commandLine.getActiveCommandLine())
  }

  // Like Vim, the cursor goes to the last line, on the character before the command line cursor (`:help c_CTRL-F`)
  @Test
  fun `ctrl-f puts the cursor on the last line before the command line cursor`() {
    enterCommand("set nodigraph")

    typeText(":abcdef<Left><Left><C-F>")

    assertEquals("set nodigraph\n".length + "abc".length, caretOffsetIn(openedCmdwin()))
  }

  @Test
  fun `enter on the line opened by ctrl-f executes it against the original editor`() {
    enterCommand("set digraph")

    typeText(":set nodigraph<C-F>")
    typeInCmdwin(openedCmdwin(), "<CR>")

    assertEquals(0, openCmdwinCount(), "cmdwin should be closed after <CR>")
    assertFalse(isDigraphEnabled(), "set nodigraph from the cmdwin should have been executed")
  }

  // Closing the cmdwin moves the selection back to the original editor, but the platform reports that asynchronously,
  // after the command has already run, and in two steps: from the cmdwin to no editor, and then to the original editor.
  // The redraw for these selection changes must not close the command's output
  @Test
  fun `selection change from closing the cmdwin keeps the output of the executed command`() {
    typeText(":echo 'hello'<C-F>")
    val cmdwin = openedCmdwin()

    val cmdwinEditor = selectedFileEditor(cmdwin)
    typeInCmdwin(cmdwin, "<CR>")

    fireSelectionChanged(from = cmdwinEditor, to = null)
    fireSelectionChanged(from = null, to = selectedFileEditor(fixture.file.virtualFile))

    assertExOutput("hello")
  }

  @Test
  fun `qa still starts macro recording`() {
    typeText("qa")
    assertTrue(injector.registerGroup.isRecording)
    typeText("q")
    assertFalse(injector.registerGroup.isRecording)
  }

  private fun typeInCmdwin(cmdwin: VirtualFile, keys: String) {
    ApplicationManager.getApplication().invokeAndWait {
      VimTestCase.typeText(injector.parser.parseKeys(keys), openTextEditor(cmdwin), fixture.project)
    }
  }

  private fun caretOffsetIn(file: VirtualFile): Int {
    var offset = -1
    ApplicationManager.getApplication().invokeAndWait { offset = openTextEditor(file).caretModel.offset }
    return offset
  }

  private fun isDigraphEnabled(): Boolean =
    injector.optionGroup.getOptionValue(
      injector.optionGroup.getOption("digraph")!!,
      OptionAccessScope.EFFECTIVE(fixture.editor.vim),
    ).toVimNumber().booleanValue

  private fun selectedFileEditor(file: VirtualFile): FileEditor =
    FileEditorManager.getInstance(fixture.project).getSelectedEditor(file)!!

  /** Sends the selection change in its own EDT event, the way the platform does */
  private fun fireSelectionChanged(from: FileEditor?, to: FileEditor?) {
    val provider = TextEditorProvider.getInstance()
    val event = FileEditorManagerEvent(
      FileEditorManager.getInstance(fixture.project),
      from?.let { FileEditorWithProvider(it, provider) },
      to?.let { FileEditorWithProvider(it, provider) },
    )
    ApplicationManager.getApplication().invokeAndWait {
      fixture.project.messageBus.syncPublisher(FileEditorManagerListener.FILE_EDITOR_MANAGER).selectionChanged(event)
    }
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

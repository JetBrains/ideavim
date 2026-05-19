/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.LightVirtualFile
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.HistoryWindowKind
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.SearchWindowGroup
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.helper.CmdwinKeys
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimEditor
import javax.swing.SwingConstants

class IjSearchWindowGroup : SearchWindowGroup {

  override fun openCommandHistoryWindow(editor: VimEditor, context: ExecutionContext) {
    if (refuseIfAlreadyOpen(context, editor)) return
    open(context, editor, HistoryWindowKind.Command, historyContent(VimHistory.Type.Command))
  }

  override fun openSearchHistoryWindow(
    editor: VimEditor,
    context: ExecutionContext,
    direction: Direction,
  ) {
    if (refuseIfAlreadyOpen(context, editor)) return
    open(context, editor, HistoryWindowKind.Search(direction), historyContent(VimHistory.Type.Search))
  }

  /**
   * Vim's `:help cmdwin`: "Recursive use of the command-line window is not possible." We surface
   * E11 (the error Vim shows for invalid actions inside the cmdwin) and decline to open a second one.
   */
  private fun refuseIfAlreadyOpen(context: ExecutionContext, editor: VimEditor): Boolean {
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context) ?: return false
    val alreadyOpen = FileEditorManager.getInstance(project).openFiles
      .any { it.getUserData(CmdwinKeys.KIND) != null }
    if (!alreadyOpen) return false
    injector.messages.showErrorMessage(editor, "E1292: Command-line window is already open")
    return true
  }

  override fun executeCurrentLineAndClose(
    cmdwin: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
  ) {
    val kind = cmdwin.getHistoryWindowKind() ?: return
    val lineStart = cmdwin.getLineStartForOffset(caret.offset)
    val lineEnd = cmdwin.getLineEndForOffset(caret.offset)
    val line = cmdwin.text().subSequence(lineStart, lineEnd).toString()

    // Resolve the original editor BEFORE closing the cmdwin (`:help cmdwin-execute`).
    val originalEditor = cmdwin.getCmdwinOriginalEditor() ?: cmdwin
    val originalContext = injector.executionContextManager.getEditorExecutionContext(originalEditor)

    // Vim's cmdwin always closes on <CR>, even when the current line is empty.
    close(cmdwin)

    if (line.isBlank()) return
    when (kind) {
      HistoryWindowKind.Command -> {
        injector.vimscriptExecutor.execute(line, originalEditor, originalContext, skipHistory = false)
      }
      is HistoryWindowKind.Search -> {
        val startCaret = originalEditor.primaryCaret()
        val result = injector.searchGroup.processSearchCommand(
          originalEditor, line, startCaret.offset, 1, kind.direction,
        )
        if (result != null) {
          originalEditor.primaryCaret().moveToOffset(result.first)
        }
      }
    }
  }

  private fun historyContent(type: VimHistory.Type): String =
    injector.historyGroup.getEntries(type, 0, 0).joinToString(separator = "\n") { it.entry }

  private fun open(
    context: ExecutionContext,
    originalEditor: VimEditor,
    kind: HistoryWindowKind,
    content: String,
  ) {
    val project = PlatformDataKeys.PROJECT.getData((context as IjEditorExecutionContext).context) ?: return
    val ijOriginalEditor = (originalEditor as IjVimEditor).editor
    val file = LightVirtualFile(kind.fileName, PlainTextFileType.INSTANCE, content)
    file.putUserData(CmdwinKeys.KIND, kind)
    ijOriginalEditor.virtualFile?.let { file.putUserData(CmdwinKeys.ORIGINAL_FILE, it) }

    val fem = FileEditorManagerEx.getInstanceEx(project)
    if (ApplicationManager.getApplication().isUnitTestMode) {
      fem.openFile(file, true)
      return
    }
    val window = fem.splitters.currentWindow
    if (window != null) {
      window.split(SwingConstants.HORIZONTAL, true, file, true)
    } else {
      fem.openFile(file, true)
    }
  }

  private fun close(cmdwin: VimEditor) {
    val ijEditor = (cmdwin as IjVimEditor).editor
    val virtualFile = ijEditor.virtualFile ?: return
    val project = ijEditor.project ?: return
    val fem = FileEditorManagerEx.getInstanceEx(project)
    // Close the cmdwin in its hosting split so the split collapses. Plain
    // `FileEditorManager.closeFile` would leave an empty pane that the platform refills
    // with the most-recently-active file, producing two copies of the original editor.
    val hostingWindows = fem.windows.filter { it.selectedComposite?.file == virtualFile }
    if (hostingWindows.isEmpty()) {
      fem.closeFile(virtualFile)
    } else {
      hostingWindows.forEach { it.closeFile(virtualFile) }
    }
  }
}

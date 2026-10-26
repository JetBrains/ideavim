/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.SearchWindowGroup
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.history.VimHistory

class IjSearchWindowGroup : SearchWindowGroup {

  override fun openCommandHistoryWindow(editor: VimEditor, context: ExecutionContext) {
    injector.virtualBufferGroup.open(
      context,
      editor,
      VirtualBufferKind.Command,
      historyContent(VimHistory.Type.Command)
    )
  }

  override fun openSearchHistoryWindow(
    editor: VimEditor,
    context: ExecutionContext,
    direction: Direction,
  ) {
    injector.virtualBufferGroup.open(
      context,
      editor,
      VirtualBufferKind.Search(direction),
      historyContent(VimHistory.Type.Search)
    )
  }

  override fun openFromCommandLine(editor: VimEditor, context: ExecutionContext, commandLine: Argument.ExString) {
    val kind = cmdwinKindOf(commandLine.label)
    val content = withLastLine(historyContent(historyTypeOf(kind)), commandLine.string)
    val cmdwin = injector.virtualBufferGroup.open(context, editor, kind, content) ?: return
    val lastLineStart = content.length - commandLine.string.length
    cmdwin.primaryCaret().moveToOffset(lastLineStart + normalModeColumn(commandLine.caretOffset))
  }

  override fun executeCurrentLineAndClose(
    cmdwin: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
  ) {
    val kind = cmdwin.getVirtualBufferKind() ?: return
    val lineStart = cmdwin.getLineStartForOffset(caret.offset)
    val lineEnd = cmdwin.getLineEndForOffset(caret.offset)
    val line = cmdwin.text().subSequence(lineStart, lineEnd).toString()

    // Resolve the original editor BEFORE closing the cmdwin (`:help cmdwin-execute`).
    val originalEditor = cmdwin.getCmdwinOriginalEditor() ?: cmdwin
    val originalContext = injector.executionContextManager.getEditorExecutionContext(originalEditor)

    injector.virtualBufferGroup.close(cmdwin)

    if (line.isBlank()) return
    when (kind) {
      VirtualBufferKind.Command -> {
        injector.vimscriptExecutor.execute(line, originalEditor, originalContext, skipHistory = false)
      }

      is VirtualBufferKind.Search -> {
        val startCaret = originalEditor.primaryCaret()
        val result = injector.searchGroup.processSearchCommand(
          originalEditor, line, startCaret.offset, 1, kind.direction,
        )
        if (result != null) {
          originalEditor.primaryCaret().moveToOffset(result.first)
        }
      }

      VirtualBufferKind.ControlCharsEditor, VirtualBufferKind.SubstitutePreview, VirtualBufferKind.Help,
      VirtualBufferKind.QuickFix -> {}
    }
  }

  private fun cmdwinKindOf(commandLineLabel: Char): VirtualBufferKind = when (commandLineLabel) {
    ':' -> VirtualBufferKind.Command
    '/' -> VirtualBufferKind.Search(Direction.FORWARDS)
    '?' -> VirtualBufferKind.Search(Direction.BACKWARDS)
    else -> error("No command-line window for the command line '$commandLineLabel'")
  }

  private fun historyTypeOf(kind: VirtualBufferKind): VimHistory.Type =
    if (kind is VirtualBufferKind.Search) VimHistory.Type.Search else VimHistory.Type.Command

  private fun withLastLine(content: String, line: String): String = if (content.isEmpty()) line else "$content\n$line"

  /**
   * The cmdwin starts in Normal mode, so the cursor goes on the character before the command line caret, like leaving
   * Insert mode does
   */
  private fun normalModeColumn(commandLineCaretOffset: Int): Int = maxOf(0, commandLineCaretOffset - 1)

  private fun historyContent(type: VimHistory.Type): String =
    injector.historyGroup.getEntries(type, 0, 0).joinToString(separator = "\n") { it.entry }
}

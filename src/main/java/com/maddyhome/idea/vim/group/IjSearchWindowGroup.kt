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

      VirtualBufferKind.ControlCharsEditor, VirtualBufferKind.SubstitutePreview -> {}
    }
  }

  private fun historyContent(type: VimHistory.Type): String =
    injector.historyGroup.getEntries(type, 0, 0).joinToString(separator = "\n") { it.entry }
}

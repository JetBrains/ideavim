/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.vimscript.model.commands.quickfix.showPosition

/**
 * The window `:copen` opens: one `name|line col column| text` line per quickfix item, with the caret on the
 * current item.
 */
internal object QuickFixWindow {

  fun open(editor: VimEditor, items: List<QuickFixItem>, currentIndex: Int) {
    injector.virtualBufferGroup.open(contextOf(editor), editor, QuickFixBuffer, render(items), currentIndex)
  }

  /**
   * When [editor] is the quickfix window itself, moves back to the window it was opened from.
   */
  fun close(editor: VimEditor) {
    val group = injector.virtualBufferGroup
    val returnTo = if (editor.getVirtualBufferKind() == QuickFixBuffer) {
      group.selectOriginalWindow(editor) ?: editor.getCmdwinOriginalEditor()
    } else {
      null
    }
    group.close(contextOf(editor), QuickFixBuffer)
    returnTo?.let { selectEditor(it) }
  }

  private fun selectEditor(editor: VimEditor) {
    val path = editor.getPath() ?: return
    val protocol = editor.extractProtocol() ?: return
    injector.file.selectEditor(editor.projectId, path, protocol)
  }

  /**
   * Shows the new list and current item if the window is open.
   */
  fun refresh(editor: VimEditor, items: List<QuickFixItem>, currentIndex: Int) {
    injector.virtualBufferGroup.refresh(contextOf(editor), QuickFixBuffer, render(items), currentIndex)
  }

  private fun render(items: List<QuickFixItem>): String =
    items.joinToString("\n") { "${it.name}|${it.line + 1} col ${it.col + 1}| ${it.text.trimStart()}" }

  private fun contextOf(editor: VimEditor): ExecutionContext =
    injector.executionContextManager.getEditorExecutionContext(editor)
}

/**
 * The read-only window `:copen` opens to show the quickfix list. An already open one is reused.
 *
 * `<CR>` jumps to the item on the caret line, in the window the quickfix window was opened from.
 */
data object QuickFixBuffer : VirtualBufferKind(), VirtualBufferKind.EnterHandler {
  override val fileName: String = "[Quickfix List]"
  override val readOnly: Boolean = true
  override val reuseOpen: Boolean = true

  override fun onEnter(buffer: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext) {
    // Read the line before moving to the other window
    val number = caret.getBufferPosition().line + 1
    val from = injector.virtualBufferGroup.selectOriginalWindow(buffer) ?: buffer.getCmdwinOriginalEditor() ?: buffer
    val jump = injector.quickFixService.jumpToItem(from, number)
    if (jump is QuickFixJump.Moved) showPosition(jump)
  }
}

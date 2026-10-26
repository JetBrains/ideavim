/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * The window `:copen` opens: one `name|line col column| text` line per quickfix item, with the caret on the
 * current item.
 */
internal object QuickFixWindow {

  fun open(editor: VimEditor, items: List<QuickFixItem>, currentIndex: Int) {
    injector.virtualBufferGroup.openQuickFix(contextOf(editor), editor, render(items), currentIndex)
  }

  /**
   * Shows the new list and current item if the window is open.
   */
  fun refresh(editor: VimEditor, items: List<QuickFixItem>, currentIndex: Int) {
    injector.virtualBufferGroup.refreshQuickFix(contextOf(editor), render(items), currentIndex)
  }

  private fun render(items: List<QuickFixItem>): String =
    items.joinToString("\n") { "${it.name}|${it.line + 1} col ${it.col + 1}| ${it.text.trimStart()}" }

  private fun contextOf(editor: VimEditor): ExecutionContext =
    injector.executionContextManager.getEditorExecutionContext(editor)
}

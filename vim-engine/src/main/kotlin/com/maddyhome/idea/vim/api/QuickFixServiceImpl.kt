/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Registered as the application service directly: the quickfix list needs nothing from the IDE.
 */
class QuickFixServiceImpl : QuickFixService {

  private var items: List<QuickFixItem> = emptyList()
  private var currentIndex = 0

  override fun replaceList(items: List<QuickFixItem>) {
    this.items = items.toList()
    currentIndex = 0
  }

  override fun jumpToFirst(editor: VimEditor): QuickFixJump = jumpTo(editor) { 0 }

  override fun jumpForward(editor: VimEditor, count: Int): QuickFixJump = jumpTo(editor) {
    if (currentIndex == items.lastIndex) null else minOf(currentIndex + count, items.lastIndex)
  }

  override fun jumpBackward(editor: VimEditor, count: Int): QuickFixJump = jumpTo(editor) {
    if (currentIndex == 0) null else maxOf(currentIndex - count, 0)
  }

  override fun reset() {
    replaceList(emptyList())
  }

  /**
   * @param newIndex the index to make current, or null if there's no item to move to
   */
  private fun jumpTo(editor: VimEditor, newIndex: () -> Int?): QuickFixJump {
    if (items.isEmpty()) return QuickFixJump.EmptyList
    currentIndex = newIndex() ?: return QuickFixJump.NoMoreItems
    val item = items[currentIndex]
    return QuickFixJump.Moved(openItem(editor, item), item, number = currentIndex + 1, size = items.size)
  }

  private fun openItem(editor: VimEditor, item: QuickFixItem): VimEditor? {
    injector.jumpService.saveJumpLocation(editor)
    val target = injector.file.selectEditor(editor.projectId, item.filepath, item.protocol) ?: return null
    target.currentCaret().moveToOffset(target.bufferPositionToOffset(BufferPosition(item.line, item.col)))
    return target
  }
}

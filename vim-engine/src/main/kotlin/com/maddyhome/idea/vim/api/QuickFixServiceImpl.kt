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

  private val listsByProject = mutableMapOf<String, QuickFixList>()

  override fun replaceList(editor: VimEditor, items: List<QuickFixItem>) {
    listsByProject[editor.projectId] = QuickFixList(items.toList())
  }

  override fun jumpToFirst(editor: VimEditor): QuickFixJump = jumpTo(editor) { 0 }

  override fun jumpForward(editor: VimEditor, count: Int): QuickFixJump = jumpTo(editor) { list ->
    if (list.isOnLast) null else minOf(list.currentIndex + count, list.lastIndex)
  }

  override fun jumpBackward(editor: VimEditor, count: Int): QuickFixJump = jumpTo(editor) { list ->
    if (list.isOnFirst) null else maxOf(list.currentIndex - count, 0)
  }

  /**
   * Like Vim's `get_nth_valid_entry`, stops at the last file it reaches, and reports
   * no more items only when not even one file could be skipped.
   */
  override fun jumpToNextFile(editor: VimEditor, count: Int): QuickFixJump = jumpTo(editor) { list ->
    var index = list.currentIndex
    repeat(count) {
      index = list.firstIndexInNextFile(index) ?: return@jumpTo index.takeIf { it != list.currentIndex }
    }
    index
  }

  override fun removeList(projectId: String) {
    listsByProject.remove(projectId)
  }

  override fun reset() {
    listsByProject.clear()
  }

  /**
   * @param newIndex the index to make current in the editor's list, or null if there's no item to move to
   */
  private fun jumpTo(editor: VimEditor, newIndex: (QuickFixList) -> Int?): QuickFixJump {
    val list = listsByProject[editor.projectId]
    if (list == null || list.items.isEmpty()) return QuickFixJump.EmptyList
    list.currentIndex = newIndex(list) ?: return QuickFixJump.NoMoreItems
    val item = list.currentItem
    return QuickFixJump.Moved(openItem(editor, item), item, number = list.currentIndex + 1, size = list.items.size)
  }

  private fun openItem(editor: VimEditor, item: QuickFixItem): VimEditor? {
    injector.jumpService.saveJumpLocation(editor)
    val target = injector.file.selectEditor(editor.projectId, item.filepath, item.protocol) ?: return null
    target.currentCaret().moveToOffset(target.bufferPositionToOffset(BufferPosition(item.line, item.col)))
    return target
  }
}

private class QuickFixList(val items: List<QuickFixItem>) {
  var currentIndex = 0

  val lastIndex: Int get() = items.lastIndex
  val isOnFirst: Boolean get() = currentIndex == 0
  val isOnLast: Boolean get() = currentIndex == lastIndex
  val currentItem: QuickFixItem get() = items[currentIndex]

  /**
   * Index of the first item after [index] that is in another file than the item at [index].
   */
  fun firstIndexInNextFile(index: Int): Int? {
    val file = items[index].filepath
    return (index + 1..lastIndex).firstOrNull { items[it].filepath != file }
  }
}

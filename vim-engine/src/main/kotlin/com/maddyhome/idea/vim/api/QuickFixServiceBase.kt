/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

abstract class QuickFixServiceBase : QuickFixService {

  private var items: List<QuickFixItem> = emptyList()

  final override val size: Int
    get() = items.size

  final override var currentIndex: Int = 0
    private set

  final override val currentItem: QuickFixItem?
    get() = items.getOrNull(currentIndex)

  override fun replaceList(items: List<QuickFixItem>) {
    this.items = items.toList()
    currentIndex = 0
  }

  override fun moveForward(count: Int): Boolean {
    val lastIndex = items.lastIndex
    if (currentIndex >= lastIndex) return false
    currentIndex = minOf(currentIndex + count, lastIndex)
    return true
  }

  override fun jumpToCurrentItem(editor: VimEditor): VimEditor? {
    val item = currentItem ?: return null
    injector.jumpService.saveJumpLocation(editor)
    val target = injector.file.selectEditor(editor.projectId, item.filepath, item.protocol) ?: return null
    target.currentCaret().moveToOffset(target.bufferPositionToOffset(BufferPosition(item.line, item.col)))
    return target
  }

  override fun reset() {
    replaceList(emptyList())
  }
}

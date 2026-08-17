/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.mark.Jump

abstract class VimTagStackServiceBase : VimTagStackService {
  // Per-project tag stacks; oldest entry at index 0, newest at the end
  private val projectToTagStack: MutableMap<String, ArrayDeque<Jump>> = mutableMapOf()

  override fun pushTag(editor: VimEditor) {
    val virtualFile = editor.getVirtualFile() ?: return
    val position = editor.offsetToBufferPosition(editor.currentCaret().offset)
    val jump = Jump(position.line, position.column, virtualFile.path, virtualFile.protocol)
    val stack = projectToTagStack.getOrPut(editor.projectId) { ArrayDeque() }
    stack.addLast(jump)
    if (stack.size > MAX_TAG_STACK_SIZE) {
      stack.removeFirst()
    }
  }

  override fun popTag(editor: VimEditor, count: Int): Jump? {
    val stack = projectToTagStack[editor.projectId] ?: return null
    if (stack.size < count) return null
    var result: Jump? = null
    repeat(count) {
      result = stack.removeLastOrNull()
    }
    return result
  }

  override fun getTagStack(editor: VimEditor): List<Jump> {
    return projectToTagStack[editor.projectId]?.toList() ?: emptyList()
  }

  override fun resetTagStack() {
    projectToTagStack.clear()
  }

  companion object {
    const val MAX_TAG_STACK_SIZE: Int = 20
  }
}

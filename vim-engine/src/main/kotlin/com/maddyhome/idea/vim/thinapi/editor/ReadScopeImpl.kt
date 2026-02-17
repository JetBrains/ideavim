/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.editor

import com.intellij.vim.api.models.CaretId
import com.intellij.vim.api.scopes.editor.EditorAccessor
import com.intellij.vim.api.scopes.editor.ReadScope
import com.intellij.vim.api.scopes.editor.caret.CaretRead
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.thinapi.caretId
import com.maddyhome.idea.vim.thinapi.editor.caret.CaretReadImpl

class ReadScopeImpl(
  private val projectId: String?,
) : ReadScope, EditorAccessor by EditorAccessorImpl(projectId) {
  private val vimEditor: VimEditor
    get() = projectId?.let { injector.editorGroup.getSelectedEditor(it) } ?: injector.fallbackWindow

  override fun <T> forEachCaret(block: CaretRead.() -> T): List<T> {
    return vimEditor.sortedCarets().map { caret -> CaretReadImpl(projectId, caret.caretId).block() }
  }

  override fun <T> with(
    caretId: CaretId,
    block: CaretRead.() -> T,
  ): T {
    return CaretReadImpl(projectId, caretId).block()
  }

  override fun <T> withPrimaryCaret(block: CaretRead.() -> T): T {
    return CaretReadImpl(projectId, vimEditor.primaryCaret().caretId).block()
  }
}
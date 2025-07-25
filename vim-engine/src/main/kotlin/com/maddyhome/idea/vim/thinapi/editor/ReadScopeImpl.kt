/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.editor

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.scopes.editor.Read
import com.intellij.vim.api.scopes.editor.ReadScope
import com.intellij.vim.api.scopes.editor.caret.CaretRead
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.caretId
import com.maddyhome.idea.vim.thinapi.editor.caret.CaretReadImpl

class ReadScopeImpl(
  listenerOwner: ListenerOwner,
  mappingOwner: MappingOwner,
) : ReadScope, Read by ReadImpl(listenerOwner, mappingOwner) {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override suspend fun <T> forEachCaret(block: suspend CaretRead.() -> T): List<T> {
    return vimEditor.sortedCarets().map { caret -> CaretReadImpl(caret.caretId).block() }
  }

  override suspend fun with(
    caretId: CaretId,
    block: suspend CaretRead.() -> Unit,
  ) {
    CaretReadImpl(caretId).block()
  }

  override suspend fun withPrimaryCaret(block: suspend CaretRead.() -> Unit) {
    CaretReadImpl(vimEditor.primaryCaret().caretId).block()
  }
}
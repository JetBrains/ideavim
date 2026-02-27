/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.VimDocument
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimEditorGroup

/**
 * No-op [VimEditorGroup] for the backend in split mode.
 * The backend has no open editors — they all live on the frontend.
 */
class BackendEditorGroup : VimEditorGroup {
  override fun notifyIdeaJoin(editor: VimEditor) {}
  override fun getEditorsRaw(): Collection<VimEditor> = emptyList()
  override fun getEditors(): Collection<VimEditor> = emptyList()
  override fun getEditors(buffer: VimDocument): Collection<VimEditor> = emptyList()
  override fun updateCaretsVisualAttributes(editor: VimEditor) {}
  override fun updateCaretsVisualPosition(editor: VimEditor) {}
  override fun getFocusedEditor(): VimEditor? = null
}

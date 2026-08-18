/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.ij
import java.util.*

class VimWindowIdService : com.maddyhome.idea.vim.api.VimWindowIdService {

  private val ids = WeakHashMap<EditorWindow, String>()
  private var counter = 0

  override fun getWindowId(editor: VimEditor): String? {
    val window = EditorHelper.getOwningEditorWindow(editor.ij) ?: return null
    return idFor(window)
  }

  private fun idFor(window: EditorWindow): String {
    return ids.computeIfAbsent(window) { "vim-window-${counter++}" }
  }
}

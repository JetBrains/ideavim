/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

abstract class EngineEditorHelperBase : EngineEditorHelper {
  override fun pad(editor: VimEditor, line: Int, to: Int): String {
    val len: Int = editor.lineLength(line)
    if (len >= to) return ""

    val limit = to - len
    return editor.indentConfig.createIndentBySize(limit)
  }
}
/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import java.lang.Long.toHexString

abstract class VimFileBase : VimFile {
  override fun displayHexInfo(editor: VimEditor) {
    val offset = editor.currentCaret().offset
    val ch = editor.text()[offset]

    injector.messages.showStatusBarMessage(editor, toHexString(ch.code.toLong()))
  }
}

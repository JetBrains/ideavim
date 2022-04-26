package com.maddyhome.idea.vim.api

import java.lang.Long.toHexString

abstract class VimFileBase : VimFile {
  override fun displayHexInfo(editor: VimEditor) {
    val offset = editor.currentCaret().offset.point
    val ch = editor.text()[offset]

    injector.messages.showStatusBarMessage(toHexString(ch.code.toLong()))
  }
}

package com.maddyhome.idea.vim.api

interface VimClipboardManager {
  /**
   * Puts the supplied text into the system clipboard
   */
  fun setClipboardText(text: String, rawText: String = text, transferableData: List<Any>): Any?
}
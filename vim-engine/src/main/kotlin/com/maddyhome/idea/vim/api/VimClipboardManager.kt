package com.maddyhome.idea.vim.api

interface VimClipboardManager {
  /**
   * Returns the string currently on the system clipboard.
   *
   * @return The clipboard string or null if data isn't plain text
   */
  fun getClipboardTextAndTransferableData(): Pair<String, List<Any>?>?

  /**
   * Puts the supplied text into the system clipboard
   */
  fun setClipboardText(text: String, rawText: String = text, transferableData: List<Any>): Any?
}
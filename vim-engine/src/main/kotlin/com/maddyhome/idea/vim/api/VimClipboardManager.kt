package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange

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

  fun getTransferableData(vimEditor: VimEditor, textRange: TextRange, text: String): List<Any>

  fun preprocessText(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String,
    transferableData: List<*>,
  ): String
}

/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange

public interface VimClipboardManager {
  /**
   * Returns the string currently on the system clipboard.
   *
   * @return The clipboard string or null if data isn't plain text
   */
  public fun getClipboardTextAndTransferableData(): Pair<String, List<Any>?>?

  /**
   * Puts the supplied text into the system clipboard
   */
  public fun setClipboardText(text: String, rawText: String = text, transferableData: List<Any>): Any?

  public fun getTransferableData(vimEditor: VimEditor, textRange: TextRange, text: String): List<Any>

  public fun preprocessText(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String,
    transferableData: List<*>,
  ): String
}

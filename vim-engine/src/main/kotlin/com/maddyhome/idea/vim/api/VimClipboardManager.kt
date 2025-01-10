/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimCopiedText
import java.awt.datatransfer.Transferable

/**
 * Interface representing a clipboard manager for the Vim text editor.
 * Vim supports two types of clipboards (or selections):
 * - **Primary**: This is a concept specific to Linux. It stores the most recently visually selected text and pastes its content on a middle mouse click.
 * - **Clipboard**: This is supported by all operating systems. It functions as a storage for the common 'copy and paste' operations typically done with Ctrl-C and Ctrl-V.
 */
interface VimClipboardManager {
  @Deprecated("Please use com.maddyhome.idea.vim.api.VimClipboardManager#getPrimaryTextAndTransferableData")
  fun getPrimaryTextAndTransferableData(): Pair<String, List<Any>?>?
  fun getPrimaryContent(editor: VimEditor, context: ExecutionContext): VimCopiedText?

  /**
   * Returns the string currently on the system clipboard.
   *
   * @return The clipboard string or null if data isn't plain text
   */
  @Deprecated("Please use com.maddyhome.idea.vim.api.VimClipboardManager#getClipboardTextAndTransferableData")
  fun getClipboardTextAndTransferableData(): Pair<String, List<Any>?>?
  fun getClipboardContent(editor: VimEditor, context: ExecutionContext): VimCopiedText?

  fun setClipboardContent(editor: VimEditor, context: ExecutionContext, textData: VimCopiedText): Boolean
  fun setPrimaryContent(editor: VimEditor, context: ExecutionContext, textData: VimCopiedText): Boolean

  @Deprecated("Please use com.maddyhome.idea.vim.api.VimClipboardManager#setClipboardText")
  fun setClipboardText(text: String, rawText: String = text, transferableData: List<Any>): Transferable?

  @Deprecated("Please use com.maddyhome.idea.vim.api.VimClipboardManager#setPrimaryText")
  fun setPrimaryText(text: String, rawText: String = text, transferableData: List<Any>): Transferable?

  fun collectCopiedText(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    text: String = editor.getText(range),
  ): VimCopiedText

  fun dumbCopiedText(text: String): VimCopiedText // TODO this method is NOT preffered, it does not collect transferableData

  fun getTransferableData(vimEditor: VimEditor, textRange: TextRange): List<Any>

  fun preprocessText(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String,
    transferableData: List<*>,
  ): String
}


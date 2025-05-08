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
import com.maddyhome.idea.vim.helper.RWLockLabel
import java.awt.datatransfer.Transferable

/**
 * Interface representing a clipboard manager for the Vim text editor.
 * Vim supports two types of clipboards (or selections):
 * - **Primary**: This is a concept specific to Linux. It stores the most recently visually selected text and pastes its content on a middle mouse click.
 * - **Clipboard**: This is supported by all operating systems. It functions as a storage for the common 'copy and paste' operations typically done with Ctrl-C and Ctrl-V.
 */
interface VimClipboardManager {
  fun getPrimaryContent(editor: VimEditor, context: ExecutionContext): VimCopiedText?

  fun getClipboardContent(editor: VimEditor, context: ExecutionContext): VimCopiedText?

  fun setClipboardContent(editor: VimEditor, context: ExecutionContext, textData: VimCopiedText): Boolean
  fun setPrimaryContent(editor: VimEditor, context: ExecutionContext, textData: VimCopiedText): Boolean

  @Deprecated("Please use com.maddyhome.idea.vim.api.VimClipboardManager#setClipboardContent")
  fun setClipboardText(text: String, rawText: String = text, transferableData: List<Any>): Transferable?

  fun collectCopiedText(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    text: String = editor.getText(range),
  ): VimCopiedText

  fun dumbCopiedText(text: String): VimCopiedText // TODO this method is NOT preffered, it does not collect transferableData

  @RWLockLabel.Readonly
  fun getTransferableData(vimEditor: VimEditor, textRange: TextRange): List<Any>

  fun preprocessText(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String,
    transferableData: List<*>,
  ): String
}


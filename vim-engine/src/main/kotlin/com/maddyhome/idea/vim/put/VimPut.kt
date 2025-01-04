/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.helper.RWLockLabel

interface VimPut {
  fun doIndent(editor: VimEditor, caret: VimCaret, context: ExecutionContext, startOffset: Int, endOffset: Int): Int

  fun notifyAboutIdeaPut(editor: VimEditor?)

  @RWLockLabel.SelfSynchronized
  fun putTextAndSetCaretPosition(
    editor: VimEditor,
    context: ExecutionContext,
    text: ProcessedTextData,
    data: PutData,
    additionalData: Map<String, Any>,
  )

  @RWLockLabel.SelfSynchronized
  fun putText(
    editor: VimEditor,
    context: ExecutionContext,
    data: PutData,
    updateVisualMarks: Boolean = false,
    modifyRegister: Boolean = true,
  ): Boolean

  @RWLockLabel.SelfSynchronized
  fun putTextForCaret(editor: VimEditor, caret: VimCaret, context: ExecutionContext, data: PutData, updateVisualMarks: Boolean = false, modifyRegister: Boolean = true): Boolean

  @RWLockLabel.SelfSynchronized
  fun putTextViaIde(
    pasteProvider: VimPasteProvider,
    vimEditor: VimEditor,
    vimContext: ExecutionContext,
    text: ProcessedTextData,
    selectionType: SelectionType,
    data: PutData,
    additionalData: Map<String, Any>,
  )

  fun getProviderForPasteViaIde(
    editor: VimEditor,
    typeInRegister: SelectionType,
    data: PutData,
  ): VimPasteProvider?
}

interface VimPasteProvider

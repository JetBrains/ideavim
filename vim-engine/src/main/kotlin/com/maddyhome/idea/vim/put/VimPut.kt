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
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.RWLockLabel

public interface VimPut {
  public fun doIndent(editor: VimEditor, caret: VimCaret, context: ExecutionContext, startOffset: Int, endOffset: Int): Int

  public fun notifyAboutIdeaPut(editor: VimEditor?)
  @RWLockLabel.SelfSynchronized
  public fun putTextAndSetCaretPosition(
    editor: VimEditor,
    context: ExecutionContext,
    text: ProcessedTextData,
    data: PutData,
    additionalData: Map<String, Any>,
  )

  @RWLockLabel.SelfSynchronized
  public fun putText(
    editor: VimEditor,
    context: ExecutionContext,
    data: PutData,
    operatorArguments: OperatorArguments,
    updateVisualMarks: Boolean = false,
    modifyRegister: Boolean = true,
  ): Boolean

  @RWLockLabel.SelfSynchronized
  public fun putTextForCaret(editor: VimEditor, caret: VimCaret, context: ExecutionContext, data: PutData, updateVisualMarks: Boolean = false, modifyRegister: Boolean = true): Boolean

  @RWLockLabel.SelfSynchronized
  public fun putTextViaIde(
    pasteProvider: VimPasteProvider,
    vimEditor: VimEditor,
    vimContext: ExecutionContext,
    text: ProcessedTextData,
    subMode: VimStateMachine.SubMode,
    data: PutData,
    additionalData: Map<String, Any>,
  )

  public fun getProviderForPasteViaIde(
    editor: VimEditor,
    typeInRegister: SelectionType,
    data: PutData,
  ): VimPasteProvider?
}

public interface VimPasteProvider

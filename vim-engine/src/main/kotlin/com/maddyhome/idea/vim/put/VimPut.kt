package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments

interface VimPut {
  fun doIndent(editor: VimEditor, caret: VimCaret, context: ExecutionContext, startOffset: Int, endOffset: Int): Int

  fun notifyAboutIdeaPut(editor: VimEditor?)
  fun putTextAndSetCaretPosition(
    editor: VimEditor,
    context: ExecutionContext,
    text: ProcessedTextData,
    data: PutData,
    additionalData: Map<String, Any>,
  )

  fun putText(
    editor: VimEditor,
    context: ExecutionContext,
    data: PutData,
    operatorArguments: OperatorArguments,
    updateVisualMarks: Boolean = false,
  ): Boolean

  fun putTextForCaret(editor: VimEditor, caret: VimCaret, context: ExecutionContext, data: PutData, updateVisualMarks: Boolean = false): Boolean
}

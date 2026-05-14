/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.functextobj

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.psi.MethodPsiRanges
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.state.mode.Mode
import kotlin.math.max

internal enum class FuncRange { OUTER_NO_DOC, OUTER_WITH_DOC, INNER }

internal class FuncTextObjectHandler(private val rangeKind: FuncRange) : ExtensionHandler {
  override val isRepeatable: Boolean get() = false

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val action = MethodRangeActionHandler(rangeKind)
    if (editor.mode is Mode.OP_PENDING) {
      KeyHandler.getInstance().keyHandlerState.commandBuilder.addAction(action)
      return
    }
    val count = max(1, operatorArguments.count0)
    editor.nativeCarets().forEach { caret: VimCaret ->
      val range = action.getRange(editor, caret, context, count, operatorArguments.count0) ?: return@forEach
      applyRange(editor, caret, range)
    }
  }

  private fun applyRange(editor: VimEditor, caret: VimCaret, range: TextRange) {
    SelectionVimListenerSuppressor.lock().use { _ ->
      if (editor.mode is Mode.VISUAL) {
        caret.vimSetSelection(range.startOffset, range.endOffset - 1, true)
      } else {
        (caret as IjVimCaret).caret.moveToInlayAwareOffset(range.startOffset)
      }
    }
  }
}

internal class MethodRangeActionHandler(private val rangeKind: FuncRange) : TextObjectActionHandler() {
  override val visualType: TextObjectVisualType get() = TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    val ijEditor = (editor as IjVimEditor).editor
    val offset = (caret as IjVimCaret).caret.offset
    val ranges = MethodPsiRanges.find(ijEditor, offset) ?: return null
    return when (rangeKind) {
      FuncRange.OUTER_NO_DOC -> TextRange(ranges.definitionStart, ranges.end)
      FuncRange.OUTER_WITH_DOC -> TextRange(ranges.fullStart, ranges.end)
      FuncRange.INNER -> ranges.body?.let { TextRange(it.first, it.second) }
    }
  }
}

/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.classtextobj

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.psi.ClassRangeFinder
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.state.mode.Mode
import kotlin.math.max

internal class ClassTextObjectHandler : ExtensionHandler {
  override val isRepeatable: Boolean get() = false

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val action = ClassRangeActionHandler()
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
    SelectionVimListenerSuppressor.lock {
      if (editor.mode is Mode.VISUAL) {
        caret.vimSetSelection(range.startOffset, range.endOffset - 1, true)
      } else {
        (caret as IjVimCaret).caret.moveToInlayAwareOffset(range.startOffset)
      }
    }
  }
}

internal class ClassRangeActionHandler : TextObjectActionHandler() {
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
    val range = ClassRangeFinder.find(ijEditor, offset) ?: return null
    return TextRange(range.first, range.second)
  }
}

/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjuser

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import kotlin.math.max

/**
 * Selects the text object defined by [patterns].
 *
 * Follows the operator-pending pattern used by the other text object extensions: when invoked as the motion of an
 * operator (e.g. `d`), it defers to a [TextObjUserActionHandler]; otherwise it sets the selection directly.
 */
internal class TextObjUserHandler(
  private val patterns: List<String>,
  private val isInner: Boolean,
  private val regionType: SelectionType = SelectionType.CHARACTER_WISE,
) : ExtensionHandler {
  override val isRepeatable: Boolean get() = false

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val action = TextObjUserActionHandler(patterns, isInner, regionType)
    if (editor.mode is Mode.OP_PENDING) {
      KeyHandler.getInstance().keyHandlerState.commandBuilder.addAction(action)
      return
    }
    val count = max(1, operatorArguments.count0)
    editor.nativeCarets().forEach { caret ->
      val range = action.getRange(editor, caret, context, count, operatorArguments.count0) ?: return@forEach
      applyRange(editor, caret, range)
    }
  }

  private fun applyRange(editor: VimEditor, caret: VimCaret, range: TextRange) {
    val mode = editor.mode
    SelectionVimListenerSuppressor.lock {
      if (mode is Mode.VISUAL) {
        caret.vimSetSelection(range.startOffset, range.endOffset - 1, true)
        if (mode.selectionType != regionType) {
          injector.visualMotionGroup.toggleVisual(editor, 1, 0, regionType)
        }
      } else {
        (caret as IjVimCaret).caret.moveToInlayAwareOffset(range.startOffset)
      }
    }
  }
}

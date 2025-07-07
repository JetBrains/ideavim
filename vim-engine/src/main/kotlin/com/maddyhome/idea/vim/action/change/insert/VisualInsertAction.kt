/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeLine
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Handles the 'I' command in Visual mode.
 *
 * For (linewise) Visual mode, the caret positioning follows these rules (based on observation in Vim):
 * - If text on multiple lines is selected AND the caret is on the first line (e.g., when selecting from bottom to top),
 *   the caret position remains unchanged
 * - In all other cases, the caret is moved to the start of the first selected line
 *
 * For blockwise Visual mode, it initiates insert at the start of block on each line in the selection
 */
@CommandOrMotion(keys = ["I"], modes = [Mode.VISUAL])
class VisualInsertAction : VisualOperatorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override fun executeForAllCarets(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    caretsAndSelections: Map<VimCaret, VimSelection>,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.isOneLineMode()) return false
    val vimSelection = caretsAndSelections.values.stream().findFirst().orElse(null) ?: return false
    return if (vimSelection.type == SelectionType.BLOCK_WISE) {
      injector.changeGroup.initBlockInsert(editor, context, vimSelection.toVimTextRange(false), false)
    } else {
      // For visual selections spanning multiple lines, keep caret position if it's on the first line
      // Otherwise move the caret to the start of the first selected line
      for ((caret, selection) in caretsAndSelections) {
        val range = selection.toVimTextRange()
        val posStart = editor.offsetToBufferPosition(range.startOffset)
        val nextLineStart = editor.getLineStartOffset(editor.normalizeLine(posStart.line + 1))
        if (caret.offset >= nextLineStart || nextLineStart >= range.endOffset) {
          caret.moveToOffset(injector.motion.moveCaretToLineStart(editor, posStart.line))
        }
      }
      injector.changeGroup.insertBeforeCaret(editor, context)
      true
    }
  }
}

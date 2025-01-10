/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.delete

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.util.*

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["D"], modes = [Mode.VISUAL])
class DeleteVisualLinesEndAction : VisualOperatorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.DELETE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_MOT_LINEWISE)

  override fun executeAction(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    range: VimSelection,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val vimTextRange = range.toVimTextRange(true)
    return if (range.type == SelectionType.BLOCK_WISE) {
      val starts = vimTextRange.startOffsets
      val ends = vimTextRange.endOffsets
      for (i in starts.indices) {
        if (ends[i] > starts[i]) {
          ends[i] = editor.getLineEndForOffset(starts[i])
        }
      }
      val blockRange = TextRange(starts, ends)
      injector.changeGroup.deleteRange(
        editor,
        context,
        editor.primaryCaret(),
        blockRange,
        SelectionType.BLOCK_WISE,
        false,
      )
    } else {
      val lineEndForOffset = editor.getLineEndForOffset(vimTextRange.endOffset)
      val endsWithNewLine = if (lineEndForOffset.toLong() == editor.fileSize()) 0 else 1
      val lineRange = TextRange(
        editor.getLineStartForOffset(vimTextRange.startOffset),
        lineEndForOffset + endsWithNewLine,
      )
      injector.changeGroup.deleteRange(editor, context, caret, lineRange, SelectionType.LINE_WISE, false)
    }
  }
}

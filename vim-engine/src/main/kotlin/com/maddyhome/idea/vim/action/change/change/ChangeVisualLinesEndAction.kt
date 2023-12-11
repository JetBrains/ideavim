/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.change

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
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_MOT_LINEWISE
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_MULTIKEY_UNDO
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["C"], modes = [Mode.VISUAL])
public class ChangeVisualLinesEndAction : VisualOperatorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.CHANGE

  override val flags: EnumSet<CommandFlags> = enumSetOf(FLAG_MOT_LINEWISE, FLAG_MULTIKEY_UNDO)

  override fun executeAction(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    range: VimSelection,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val vimTextRange = range.toVimTextRange(true)
    return if (range.type == SelectionType.BLOCK_WISE && vimTextRange.isMultiple) {
      val starts = vimTextRange.startOffsets
      val ends = vimTextRange.endOffsets
      for (i in starts.indices) {
        if (ends[i] > starts[i]) {
          ends[i] = editor.getLineEndForOffset(starts[i])
        }
      }
      val blockRange = TextRange(starts, ends)
      injector.changeGroup.changeRange(editor, caret, blockRange, SelectionType.BLOCK_WISE, context, operatorArguments)
    } else {
      val lineEndForOffset = editor.getLineEndForOffset(vimTextRange.endOffset)
      val endsWithNewLine = if (lineEndForOffset.toLong() == editor.fileSize()) 0 else 1
      val lineRange = TextRange(
        editor.getLineStartForOffset(vimTextRange.startOffset),
        lineEndForOffset + endsWithNewLine,
      )
      injector.changeGroup.changeRange(editor, caret, lineRange, SelectionType.LINE_WISE, context, operatorArguments)
    }
  }
}

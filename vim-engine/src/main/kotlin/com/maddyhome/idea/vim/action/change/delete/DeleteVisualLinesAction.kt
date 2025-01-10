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
@CommandOrMotion(keys = ["X"], modes = [Mode.VISUAL])
class DeleteVisualLinesAction : VisualOperatorActionHandler.ForEachCaret() {
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
    val textRange = range.toVimTextRange(false)
    val (usedCaret, usedRange, usedType) = when (range.type) {
      SelectionType.BLOCK_WISE -> Triple(editor.primaryCaret(), textRange, range.type)
      SelectionType.LINE_WISE -> Triple(caret, textRange, SelectionType.LINE_WISE)
      SelectionType.CHARACTER_WISE -> {
        val lineEndForOffset = editor.getLineEndForOffset(textRange.endOffset)
        val endsWithNewLine = if (lineEndForOffset.toLong() == editor.fileSize()) 0 else 1
        val lineRange = TextRange(
          editor.getLineStartForOffset(textRange.startOffset),
          lineEndForOffset + endsWithNewLine,
        )
        Triple(caret, lineRange, SelectionType.LINE_WISE)
      }
    }
    return injector.changeGroup.deleteRange(editor, context, usedCaret, usedRange, usedType, false)
  }
}

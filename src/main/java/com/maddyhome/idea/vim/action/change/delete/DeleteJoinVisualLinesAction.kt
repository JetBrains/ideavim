/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.delete

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * @author vlan
 */
public class DeleteJoinVisualLinesAction : VisualOperatorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.DELETE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_EXIT_VISUAL)

  override fun executeForAllCarets(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    caretsAndSelections: Map<VimCaret, VimSelection>,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.isOneLineMode()) return false
    if (injector.options(editor).isSet(IjOptions.ideajoin)) {
      injector.changeGroup.joinViaIdeaBySelections(editor, context, caretsAndSelections)
      return true
    }
    var res = true
    editor.nativeCarets().sortedByDescending { it.offset.point }.forEach { caret ->
      if (!caret.isValid) return@forEach
      val range = caretsAndSelections[caret] ?: return@forEach
      if (!injector.changeGroup.deleteJoinRange(
          editor,
          caret,
          range.toVimTextRange(true).normalize(),
          false,
          operatorArguments,
        )
      ) {
        res = false
      }
    }
    return res
  }
}

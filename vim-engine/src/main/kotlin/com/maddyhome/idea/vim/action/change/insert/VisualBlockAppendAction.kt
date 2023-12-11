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
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["A"], modes = [Mode.VISUAL])
public class VisualBlockAppendAction : VisualOperatorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override fun executeForAllCarets(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    caretsAndSelections: Map<VimCaret, VimSelection>,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.isOneLineMode()) return false
    val range = caretsAndSelections.values.stream().findFirst().orElse(null) ?: return false
    return if (range.type == SelectionType.BLOCK_WISE) {
      injector.changeGroup.blockInsert(editor, context, range.toVimTextRange(false), true, operatorArguments)
    } else {
      injector.changeGroup.insertAfterLineEnd(editor, context)
      true
    }
  }
}

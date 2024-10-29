/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.copy

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["y"], modes = [Mode.VISUAL])
class YankVisualAction : VisualOperatorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.COPY

  override fun executeForAllCarets(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    caretsAndSelections: Map<VimCaret, VimSelection>,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val selections = caretsAndSelections.values
    val starts: MutableList<Int> = ArrayList()
    val ends: MutableList<Int> = ArrayList()
    selections.forEach { selection: VimSelection ->
      val textRange = selection.toVimTextRange(false)
      textRange.startOffsets.forEach { e: Int -> starts.add(e) }
      textRange.endOffsets.forEach { e: Int -> ends.add(e) }
    }
    val vimSelection = selections.firstOrNull() ?: return false
    val startsArray = starts.toIntArray()
    val endsArray = ends.toIntArray()
    return injector.yank.yankRange(editor, context, TextRange(startsArray, endsArray), vimSelection.type, true)
  }
}

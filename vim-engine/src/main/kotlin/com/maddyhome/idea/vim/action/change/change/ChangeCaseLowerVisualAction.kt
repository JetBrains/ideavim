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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.CharacterHelper

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["u"], modes = [Mode.VISUAL])
public class ChangeCaseLowerVisualAction : VisualOperatorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.CHANGE

  override fun executeAction(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    range: VimSelection,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return injector.changeGroup
      .changeCaseRange(editor, caret, range.toVimTextRange(false), CharacterHelper.CASE_LOWER)
  }
}

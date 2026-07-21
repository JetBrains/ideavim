/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

@CommandOrMotion(keys = ["<C-^>"], modes = [Mode.INSERT])
class ToggleLanguageInsertModeAction : VimActionHandler.SingleExecution() {
  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val currentValue = injector.optionGroup.getOptionValue(
      Options.iminsert, OptionAccessScope.LOCAL(editor)
    ).value
    val newValue = if (currentValue == 0) 1 else 0
    injector.optionGroup.setOptionValue(Options.iminsert, OptionAccessScope.LOCAL(editor), VimInt(newValue));
    return true
  }

  override val type: Command.Type
    get() = Command.Type.OTHER_READONLY
}
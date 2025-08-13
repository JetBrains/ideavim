/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

@CommandOrMotion(keys = ["<Insert>"], modes = [Mode.CMD_LINE])
class ToggleInsertModeAction : CommandLineActionHandler() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_UNDO_AWARE)

  override fun execute(commandLine: VimCommandLine): Boolean {
    commandLine.toggleReplaceMode()
    return true
  }
}

/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.register.RegisterConstants

@CommandOrMotion(keys = ["<C-Y>"], modes = [Mode.CMD_LINE])
internal class CopyModelessSelectionAction : CommandLineActionHandler() {
  override fun execute(
    commandLine: VimCommandLine,
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
  ): Boolean {
    val selection = commandLine.modelessSelection
    if (selection.isNotEmpty()) {
      injector.registerGroup.storeText(editor, context, RegisterConstants.CLIPBOARD_REGISTER, selection)
    }
    return true
  }

  override fun execute(commandLine: VimCommandLine): Boolean = true
}

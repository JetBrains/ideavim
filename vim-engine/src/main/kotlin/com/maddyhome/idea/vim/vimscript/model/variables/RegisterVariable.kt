/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.variables

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * Represents the `v:register` variable
 *
 * The name of the register in effect for the current normal mode
 * command (regardless of whether that command actually used a
 * register). Or for the currently executing normal mode mapping
 * (use this in custom commands that take a register).
 * If none is supplied it is the default register '"', unless
 * 'clipboard' contains "unnamed" or "unnamedplus", then it is
 * "*" or '+' ("unnamedplus" prevails).
 *
 * See `:help v:register`
 */
internal object RegisterVariable : Variable {

  override fun evaluate(
    name: String,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val register = KeyHandler.getInstance().keyHandlerState.commandBuilder.registerSnapshot
      ?: injector.registerGroup.currentRegister
    return VimString(register.toString())
  }

}
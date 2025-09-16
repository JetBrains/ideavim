/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.register.RegisterConstants
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class RegisterExpression(val char: Char) : LValueExpression() {
  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    // TODO: We should return some kind of nullable string here
    // Assuming @a is uninitialised, Vim returns: `echo type(@a) == v:t_string` => 1, `echo @a == v:null` => 1
    // While uninitialised register is not the same as an empty register, `echo @a==''` => 1, so this is a reasonable
    // workaround until we support v:null
    val register = injector.registerGroup.getRegister(editor, context, char)
    if (register != null) {
      return VimString(injector.parser.toPrintableString(register.keys))
    }
    return VimString.EMPTY
  }

  /**
   * Register expressions are strongly typed, always String
   *
   * This means arithmetic operations on a register are not valid, only string concatenation.
   */
  override fun isStronglyTyped() = true

  override fun assign(
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) {
    if (!RegisterConstants.WRITABLE_REGISTERS.contains(char)) {
      throw exExceptionMessage("E354", char)
    }
    injector.registerGroup.storeText(editor, context, char, value.toVimString().value)
  }
}

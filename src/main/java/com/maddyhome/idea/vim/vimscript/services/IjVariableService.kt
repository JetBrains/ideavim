/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable

internal class IjVariableService : VimVariableServiceBase() {
  override fun storeVariable(variable: Variable, value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) {
    super.storeVariable(variable, value, editor, context, vimContext)

    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    if (scope == Scope.GLOBAL_VARIABLE) {
      val scopeForGlobalEnvironment = variable.scope?.toString() ?: ""
      VimScriptGlobalEnvironment.getInstance()
        .variables[scopeForGlobalEnvironment + variable.name.evaluate(editor, context, vimContext)] = value.simplify()
    }
  }

  private fun VimDataType.simplify(): Any {
    return when (this) {
      is VimString -> this.value
      is VimInt -> this.value
      is VimFloat -> this.value
      is VimList -> this.values
      is VimDictionary -> this.dictionary
      is VimBlob -> "blob"
      is VimFuncref -> "funcref"
      else -> error("Unexpected")
    }
  }
}

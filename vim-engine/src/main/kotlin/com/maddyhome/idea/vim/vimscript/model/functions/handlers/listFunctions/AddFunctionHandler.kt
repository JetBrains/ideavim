/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.listFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.BinaryFunctionHandler

@VimscriptFunction(name = "add")
internal class AddFunctionHandler : BinaryFunctionHandler<VimList>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimList {
    val list = arguments[0]
    if (list !is VimList && list !is VimBlob) {
      // TODO: Vim actually shows an error AND returns 1 here!?
      throw exExceptionMessage("E897")
    }

    // TODO: Support Blob
    // When we support Blob, we should move out of the listFunctions package, and change return type to VimDataType
    list as? VimList ?: throw ExException("Blob is not currently supported for add()")

    if (list.isLocked) {
      throw exExceptionMessage("E741", "add() argument")
    }

    // The type of the expression is not validated, but simply added to the end of the list
    val expression = arguments[1]
    list.values.add(expression)
    return list
  }
}

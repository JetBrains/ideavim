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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandlerBase

@VimscriptFunction(name = "insert")
internal class InsertFunctionHandler : FunctionHandlerBase<VimDataType>(2, 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val list = arguments[0]
    if (list !is VimList && list !is VimBlob) {
      // TODO: Vim actually shows an error AND returns 1 here!?
      throw exExceptionMessage("E899")
    }

    // TODO: Support Blob
    // When we support Blob, we should move out of the listFunctions package
    list as? VimList ?: throw ExException("Blob is not currently supported for insert()")

    val item = arguments[1]
    val idx = arguments.getNumberOrNull(2)?.value ?: 0

    val index = if (idx < 0) list.size + idx else idx
    if (index < 0 || index > list.size) {
      throw exExceptionMessage("E684", idx)
    }

    // TODO: When fixing locking, re-enable the tests in InsertFunctionTest
    // When we lock a variable, we currently lock its value, which is incorrect (especially for Number, Float and
    // String which can be represented with singletons). So locking a List or Dictionary variable also locks the items,
    // but there is no differentiation between locking add/remove items and locking the value of the items.
    if (list.isLocked) {
      throw exExceptionMessage("E741", "insert() argument")
    }

    list.values.add(index, item)
    return list
  }
}

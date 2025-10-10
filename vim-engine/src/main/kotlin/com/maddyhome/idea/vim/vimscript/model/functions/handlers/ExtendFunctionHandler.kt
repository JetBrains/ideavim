/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandlerBase

@VimscriptFunction(name = "extend")
internal class ExtendFunctionHandler : FunctionHandlerBase<VimDataType>(minArity = 2, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val argument1 = arguments[0]
    val argument2 = arguments[1]

    if (argument1 is VimList && argument2 is VimList) {
      if (argument1.isLocked) {
        throw exExceptionMessage("E741", "extend() argument")
      }
      val index = arguments.getNumberOrNull(2)?.value ?: argument1.size
      argument1.values.addAll(index, argument2.values)
      return argument1
    }

    if (argument1 is VimDictionary && argument2 is VimDictionary) {
      val argument3 = arguments.getStringOrNull(2)?.value?.lowercase() ?: "force"
      if (argument3 !in listOf("force", "keep", "error")) {
        throw exExceptionMessage("E475", argument3)
      }

      if (argument1.isLocked) {
        throw exExceptionMessage("E741", "extend() argument")
      }

      argument2.dictionary.forEach { (key, value) ->
        if (argument1.dictionary.containsKey(key)) {
          if (argument1.dictionary[key]?.isLocked == true) {
            throw exExceptionMessage("E741", "extend() argument")
          }

          when (argument3) {
            "force" -> argument1.dictionary[key] = value
            "keep" -> { /* Do nothing. Keep the value in argument1 */
            }

            "error" -> throw exExceptionMessage("E737", key.toOutputString())
          }
        } else {
          argument1.dictionary[key] = value
        }
      }

      // TODO: Should return 0 on error. However, IdeaVim does not support this and can only throw errors
      return argument1
    }

    throw exExceptionMessage("E712", "extend()")
  }
}

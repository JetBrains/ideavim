/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.statements

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.IndexedExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo

data class AnonymousFunctionDeclaration(
  val sublist: IndexedExpression,
  val args: List<String>,
  val defaultArgs: List<Pair<String, Expression>>,
  val body: List<Executable>,
  val replaceExisting: Boolean,
  val flags: Set<FunctionFlag>,
  val hasOptionalArguments: Boolean,
) : Executable {

  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    val container = sublist.expression.evaluate(editor, context, vimContext)
    if (container !is VimDictionary) {
      throw exExceptionMessage("E1203")
    }
    val index = ((sublist.index as SimpleExpression).data as VimString)
    if (container.dictionary.containsKey(index)) {
      if (container.dictionary[index] is VimFuncref && !replaceExisting) {
        throw exExceptionMessage("E717")
      } else if (container.dictionary[index] !is VimFuncref) {
        throw exExceptionMessage("E718")
      }
    }
    val declaration = FunctionDeclaration(
      null,
      VimFuncref.anonymousCounter++.toString(),
      args,
      defaultArgs,
      body,
      replaceExisting,
      flags + FunctionFlag.DICT,
      hasOptionalArguments
    )
    declaration.vimContext = this.vimContext
    container.dictionary[index] =
      VimFuncref(DefinedFunctionHandler(declaration), VimList(mutableListOf()), container, VimFuncref.Type.FUNCREF)
    container.dictionary[index]
    return ExecutionResult.Success
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    body.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}

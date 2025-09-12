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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo

data class FunctionDeclaration(
  val scope: Scope?,
  val name: String,
  val args: List<String>,
  val defaultArgs: List<Pair<String, Expression>>,
  val body: List<Executable>,
  val replaceExisting: Boolean,
  val flags: Set<FunctionFlag>,
  val hasOptionalArguments: Boolean,
) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange
  var isDeleted: Boolean = false

  /**
   * we store the "a:" and "l:" scope variables here
   * see ":h scope"
   */
  val functionVariables: MutableMap<String, VimDataType> = mutableMapOf()
  val localVariables: MutableMap<String, VimDataType> = mutableMapOf()

  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    injector.statisticsService.setIfFunctionDeclarationUsed(true)
    val forbiddenArgumentNames = setOf("firstline", "lastline")
    val forbiddenArgument = args.firstOrNull { forbiddenArgumentNames.contains(it) }
    if (forbiddenArgument != null) {
      throw exExceptionMessage("E125", forbiddenArgument)
    }

    body.forEach { it.vimContext = this }
    injector.functionService.storeFunction(this)
    return ExecutionResult.Success
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    body.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}

enum class FunctionFlag(val abbrev: String) {
  RANGE("range"),
  ABORT("abort"),
  DICT("dict"),
  CLOSURE("closure"),
  ;

  companion object {
    fun getByName(abbrev: String): FunctionFlag? {
      return entries.firstOrNull { it.abbrev == abbrev }
    }
  }
}

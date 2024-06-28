/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo

data class Script(val units: List<Executable> = ArrayList()) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  /**
   * we store the "s:" scope variables and functions here
   * see ":h scope"
   */
  val scriptVariables: MutableMap<String, VimDataType> = mutableMapOf()
  val scriptFunctions: MutableMap<String, FunctionDeclaration> = mutableMapOf()

  override fun getPreviousParentContext(): VimLContext {
    throw RuntimeException("Script has no parent context")
  }

  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    var latestResult: ExecutionResult = ExecutionResult.Success
    for (unit in units) {
      unit.vimContext = this
      if (latestResult is ExecutionResult.Success) {
        latestResult = unit.execute(editor, context)
      } else {
        break
      }
    }
    return latestResult
  }

  override fun restoreOriginalRange(deletionInfo: DeletionInfo) {
    super.restoreOriginalRange(deletionInfo)
    units.forEach { it.restoreOriginalRange(deletionInfo) }
  }
}

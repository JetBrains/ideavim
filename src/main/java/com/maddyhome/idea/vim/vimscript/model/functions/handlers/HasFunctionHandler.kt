/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.statistic.VimscriptState
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

internal class HasFunctionHandler : FunctionHandler() {
  override val name = "has"
  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 2

  private val supportedFeatures = setOf("ide")

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val feature = argumentValues[0].evaluate(editor, context, vimContext).asString()
    if (feature == "ide") {
      VimscriptState.isIDESpecificConfigurationUsed = true
    }
    return if (supportedFeatures.contains(feature)) {
      VimInt.ONE
    } else {
      VimInt.ZERO
    }
  }
}

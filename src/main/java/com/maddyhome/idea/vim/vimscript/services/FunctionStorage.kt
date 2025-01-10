/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.api.VimScriptFunctionServiceBase
import com.maddyhome.idea.vim.vimscript.model.functions.EngineFunctionProvider
import com.maddyhome.idea.vim.vimscript.model.functions.IntellijFunctionProvider
import com.maddyhome.idea.vim.vimscript.model.functions.VimscriptFunctionProvider

internal class FunctionStorage : VimScriptFunctionServiceBase() {
  override val functionProviders: List<VimscriptFunctionProvider> =
    listOf(EngineFunctionProvider, IntellijFunctionProvider)
}

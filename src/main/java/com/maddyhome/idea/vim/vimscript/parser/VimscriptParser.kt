/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser

import com.maddyhome.idea.vim.api.VimscriptParserBase
import com.maddyhome.idea.vim.vimscript.model.commands.EngineExCommandProvider
import com.maddyhome.idea.vim.vimscript.model.commands.ExCommandProvider
import com.maddyhome.idea.vim.vimscript.model.commands.IntellijExCommandProvider

internal object VimscriptParser : VimscriptParserBase() {
  override val commandProviders: List<ExCommandProvider> = listOf(EngineExCommandProvider, IntellijExCommandProvider)
}

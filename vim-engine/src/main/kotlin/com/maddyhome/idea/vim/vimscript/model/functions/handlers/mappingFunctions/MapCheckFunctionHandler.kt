/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.mappingFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getFirstMappingInfoPrefix
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimString

@VimscriptFunction("mapcheck")
internal class MapCheckFunctionHandler: MapFunctionHandlerBase<VimString>(minArity = 1, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimString {
    val name = arguments.getString(0)
    val mode = arguments.getStringOrNull(1)
    val abbr = arguments.getNumberOrNull(2)?.booleanValue ?: false

    // TODO: IdeaVim should support abbreviations
    if (abbr) {
      return VimString.EMPTY
    }

    val modes = getMappingModes(mode)
    val keys = injector.parser.parseKeys(name.value)

    val mappingInfo = injector.keyGroup.getFirstMappingInfoPrefix(keys, modes)
    return mappingInfo?.getPresentableString()?.asVimString() ?: VimString.EMPTY
  }
}

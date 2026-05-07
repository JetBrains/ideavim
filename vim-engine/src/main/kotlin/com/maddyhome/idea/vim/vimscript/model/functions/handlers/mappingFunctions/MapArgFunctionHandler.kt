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
import com.maddyhome.idea.vim.api.getFirstMappingInfoMatch
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimString

@VimscriptFunction("maparg")
internal class MapArgFunctionHandler : MapFunctionHandlerBase<VimDataType>(minArity = 1, maxArity = 4) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val name = arguments.getString(0)
    val mode = arguments.getStringOrNull(1)
    val abbr = arguments.getNumberOrNull(2)?.booleanValue ?: false
    val dict = arguments.getNumberOrNull(3)?.booleanValue ?: false

    // TODO: IdeaVim should support abbreviations
    if (abbr) {
      return if (dict) VimDictionary(LinkedHashMap()) else VimString.EMPTY
    }

    val modes = getMappingModes(mode)
    val keys = injector.parser.parseKeys(name.value)

    val mapping = injector.keyGroup.getFirstMappingInfoMatch(keys, modes)
    return if (dict) {
      getMappingAsDictionary(name, mapping)
    } else {
      mapping?.mappingInfo?.getPresentableString()?.asVimString() ?: VimString.EMPTY
    }
  }
}

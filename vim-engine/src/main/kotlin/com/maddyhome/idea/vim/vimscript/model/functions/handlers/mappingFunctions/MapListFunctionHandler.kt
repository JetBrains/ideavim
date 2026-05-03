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
import com.maddyhome.idea.vim.api.getAllMappingInfoWithMode
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimString
import java.util.*

@VimscriptFunction("maplist")
internal class MapListFunctionHandler : MapFunctionHandlerBase<VimList>(minArity = 0, maxArity = 1) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimList {
    val abbr = arguments.getNumberOrNull(0)?.booleanValue ?: false

    // TODO: IdeaVim should support abbreviations
    if (abbr) {
      return VimList(mutableListOf())
    }

    val list = mutableListOf<VimDataType>()
    val mappings = injector.keyGroup.getAllMappingInfoWithMode(emptyList(), EnumSet.allOf(MappingMode::class.java))
    mappings.forEach {
      val name = injector.parser.toKeyNotation(it.mappingInfo.fromKeys)
      val dictionary = getMappingAsDictionary(name.asVimString(), it)
      list.add(dictionary)
    }
    return VimList(list)
  }
}

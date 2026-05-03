/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.mappingFunctions

import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import java.util.EnumSet

internal abstract class MapFunctionHandlerBase<T : VimDataType>(minArity: Int = 0, maxArity: Int? = null) :
  BuiltinFunctionHandler<T>(minArity, maxArity) {

  protected fun getMappingModes(mode: VimString?): EnumSet<MappingMode> {
    return when (mode?.value?.firstOrNull()) {
      'n' -> MappingMode.N
      'v' -> MappingMode.V
      'o' -> MappingMode.O
      'i' -> MappingMode.I
      'c' -> MappingMode.C
      's' -> MappingMode.S
      'x' -> MappingMode.X
//      'l' -> MappingMode.L  // TODO: Langmap
//      't' -> MappingMode.T  // Terminal-Job. IdeaVim is unlikely to support this
      else -> MappingMode.NVO
    }
  }
}

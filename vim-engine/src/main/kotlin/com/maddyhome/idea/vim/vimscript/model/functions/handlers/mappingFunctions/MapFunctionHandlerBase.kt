/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.mappingFunctions

import com.maddyhome.idea.vim.api.MappingInfoWithMode
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MappingMode.Companion.toModeString
import com.maddyhome.idea.vim.key.ToExpressionMappingInfo
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import java.util.*

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

  protected fun getMappingAsDictionary(
    name: VimString,
    mapping: MappingInfoWithMode?,
  ): VimDictionary {
    val dictionary = VimDictionary(LinkedHashMap())
    if (mapping!= null) {
      val mappingInfo = mapping.mappingInfo
      dictionary["lhs"] = name  // As the mapping is typed, with special keys as plain text - e.g. "foo<Tab>" or "foo<Left>"
      // TODO: This should be the keys as raw bytes, i.e. with special keys encoded
      // Vim does this with something like "foo\t" or "foo<80>kl" ("foo<Left>")
      dictionary["lhsraw"] = name
      // Vim also includes "lhsrawalt" as an alternate form for the raw bytes, but only if this differs to "lhsraw".
      // It's unclear what this alternate form is, or what it can be used for other than `mapset()`.
      // Vim history appears to indicate that it's required for `mapset()` to function correctly.
      // It's not clear if IdeaVim needs this or not (we can also add more fields to this dictionary if required)
      dictionary["rhs"] = VimString(mappingInfo.getPresentableString())
      dictionary["silent"] = VimInt.ZERO  // TODO: IdeaVim does not currently support <silent> mappings
      dictionary["noremap"] = (!mappingInfo.isRecursive).asVimInt()
      dictionary["script"] = VimInt.ZERO  // TODO: IdeaVim does not currently support <script> mappings
      dictionary["expr"] = (mappingInfo is ToExpressionMappingInfo).asVimInt()
      dictionary["buffer"] = VimInt.ZERO  // TODO: IdeaVim does not currently support <buffer> local mappings
      dictionary["mode"] = VimString(mapping.modes.toModeString())
      // These are all related to <script> mappings
      // dictionary["sid"]
      // dictionary["scriptversion"]
      // dictionary["lnum"]
      dictionary["nowait"] = VimInt.ZERO  // TODO: IdeaVim does not currently support <nowait> mappings
      dictionary["abbr"] = VimInt.ZERO  // TODO: IdeaVim should support abbreviations
      // We don't support "mode_bits". It's an internal representation of "mode" that may change in the future and
      // isn't even used by mapset(). It seems to exist purely for user-disambiguation for any overlaps in "mode"
    }
    return dictionary
  }
}

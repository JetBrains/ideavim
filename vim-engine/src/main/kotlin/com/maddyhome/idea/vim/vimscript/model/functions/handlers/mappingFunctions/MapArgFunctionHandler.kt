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
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MappingMode.Companion.toModeString
import com.maddyhome.idea.vim.key.ToExpressionMappingInfo
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction("maparg")
internal class MapArgFunctionHandler : BuiltinFunctionHandler<VimDataType>(minArity = 1, maxArity = 4) {
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

    val modes = when (mode?.value?.firstOrNull()) {
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

    val keys = injector.parser.parseKeys(name.value)

    val mappingInfo = injector.keyGroup.getFirstMappingInfoMatch(keys, modes)
    if (mappingInfo == null) {
      return if (dict) VimDictionary(LinkedHashMap()) else VimString.EMPTY
    }
    else {
      return if (dict) {
        VimDictionary(LinkedHashMap()).also {
          it["lhs"] = name  // As the mapping is typed, with special keys as plain text - e.g. "foo<Tab>" or "foo<Left>"
          // TODO: This should be the keys as raw bytes, i.e. with special keys encoded
          // Vim does this with something like "foo\t" or "foo<80>kl" ("foo<Left>")
          it["lhsraw"] = name
          // Vim also includes "lhsrawalt" as an alternate form for the raw bytes, but only if this differs to "lhsraw".
          // It's unclear what this alternate form is, or what it can be used for other than `mapset()`.
          // Vim history appears to indicate that it's required for `mapset()` to function correctly.
          // It's not clear if IdeaVim needs this or not (we can also add more fields to this dictionary if required)
          it["rhs"] = VimString(mappingInfo.getPresentableString())
          it["silent"] = VimInt.ZERO  // TODO: IdeaVim does not currently support <silent> mappings
          it["noremap"] = (!mappingInfo.isRecursive).asVimInt()
          it["script"] = VimInt.ZERO  // TODO: IdeaVim does not currently support <script> mappings
          it["expr"] = (mappingInfo is ToExpressionMappingInfo).asVimInt()
          it["buffer"] = VimInt.ZERO  // TODO: IdeaVim does not currently support <buffer> local mappings
          it["mode"] = VimString(mappingInfo.originalModes.toModeString())
          // These are all related to <script> mappings
//          it["sid"]
//          it["scriptversion"]
//          it["lnum"]
          it["nowait"] = VimInt.ZERO  // TODO: IdeaVim does not currently support <nowait> mappings
          it["abbr"] = VimInt.ZERO  // TODO: IdeaVim should support abbreviations
          // We don't support "mode_bits". It's an internal representation of "mode" that may change in the future and
          // isn't even used by mapset(). It seems to exist purely for user-disambiguation for any overlaps in "mode"
        }
      }
      else {
        VimString(mappingInfo.getPresentableString())
      }
    }
  }
}

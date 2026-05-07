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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction("mapset")
internal class MapSetFunctionHandler : BuiltinFunctionHandler<VimInt>(minArity = 1, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {

    if (arguments.size > 1) {
      val mode = arguments.getString(0)
      val abbr = arguments.getNumber(1).booleanValue
      val dict = arguments.getOrNull(2) as? VimDictionary ?: throw exExceptionMessage("E1206", 3)

      // TODO: IdeaVim should support abbreviations
      if (abbr) {
        return VimInt.ZERO
      }

      setMap(mode.value, dict)
    }
    else {
      val dict = arguments[0] as? VimDictionary ?: throw exExceptionMessage("E1206", 1)
      val mode = dict["mode"] as? VimString ?: throw exExceptionMessage("E460")
      setMap(mode.value, dict)
    }

    // Interestingly, Vim documents mapset() as returning void. We don't support this...
    return VimInt.ONE
  }

  private fun setMap(mode: String, dict: VimDictionary) {
    val lhs = dict["lhs"] as? VimString ?: throw exExceptionMessage("E460")
    val rhs = dict["rhs"] as? VimString ?: throw exExceptionMessage("E460")
    val expr = (dict["expr"] as? VimInt)?.booleanValue ?: throw exExceptionMessage("E460")
    val noremap = (dict["noremap"] as? VimInt)?.booleanValue ?: throw exExceptionMessage("E460")

    val modes = MappingMode.fromModeString(mode)

    // We don't restore the mapping owner because we're setting it again by script. If anything, it should be the
    // current script calling `mapset()`
    val owner = MappingOwner.IdeaVim.Other

    // TODO: Support <script>, <buffer>, <nowait>, <silent>, etc. when IdeaVim supports them while creating a mapping

    val fromKeys = injector.parser.parseKeys(lhs.value)
    if (!expr) {
      val toKeys = injector.parser.parseKeys(rhs.value)
      injector.keyGroup.putKeyMapping(modes, fromKeys, owner, toKeys, !noremap)
    }
    else {
      val expression = injector.vimscriptParser.parseExpression(rhs.value) ?: throw exExceptionMessage("E460")
      injector.keyGroup.putKeyMapping(modes, fromKeys, owner, expression, rhs.value, !noremap)
    }
  }
}

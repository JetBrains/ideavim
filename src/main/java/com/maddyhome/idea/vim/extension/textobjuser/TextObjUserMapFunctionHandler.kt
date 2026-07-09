/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjuser

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

/**
 * Handles `textobj#user#map({name}, {specs})`: maps additional user keys onto the `<Plug>` interface names of an
 * already-defined plugin. Unlike [TextObjUserPluginFunctionHandler] it only reads the mapping properties
 * (`select` / `select-a` / `select-i` / `move-*`) and creates no new text objects.
 */
internal class TextObjUserMapFunctionHandler(
  override val name: String,
  private val owner: MappingOwner,
) : FunctionHandler {
  override val scope: Scope? = null

  override fun executeFunction(
    arguments: List<Expression>,
    range: Range?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val plugin = (arguments[0].evaluate(editor, context, vimContext) as VimString).value
    val specs = arguments[1].evaluate(editor, context, vimContext) as VimDictionary
    for ((obj, spec) in specs.dictionary) {
      mapObject(plugin, obj.value, spec as VimDictionary)
    }
    return VimString("")
  }

  private fun mapObject(plugin: String, obj: String, spec: VimDictionary) {
    // Text objects apply in Visual and Operator-pending modes; motions also in Normal mode.
    mapOperation(plugin, obj, "select", spec["select"], MappingMode.XO)
    mapOperation(plugin, obj, "select-a", spec["select-a"], MappingMode.XO)
    mapOperation(plugin, obj, "select-i", spec["select-i"], MappingMode.XO)
    mapOperation(plugin, obj, "move-n", spec["move-n"], MappingMode.NXO)
    mapOperation(plugin, obj, "move-p", spec["move-p"], MappingMode.NXO)
    mapOperation(plugin, obj, "move-N", spec["move-N"], MappingMode.NXO)
    mapOperation(plugin, obj, "move-P", spec["move-P"], MappingMode.NXO)
  }

  private fun mapOperation(plugin: String, obj: String, specName: String, keys: VimDataType?, modes: Set<MappingMode>) {
    val userKeys = keys.toStringList()
    if (userKeys.isEmpty()) return
    mapUserKeys(owner, userKeys, plugName(plugin, obj, specName), modes)
  }
}

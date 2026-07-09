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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

/**
 * Handles `textobj#user#plugin({name}, {specs})`. For each object it defines a `<Plug>(textobj-{name}-{obj}-{op})`
 * interface mapping bound to the handler, then maps the user's keys onto that `<Plug>` name so they can be remapped.
 */
internal class TextObjUserPluginFunctionHandler(
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
      registerTextObject(plugin, obj.value, spec as VimDictionary)
    }
    return VimString("")
  }

  private fun registerTextObject(plugin: String, obj: String, spec: VimDictionary) {
    // "pattern" is either a single Vim regex, or a [header, footer] pair of Vim regexes.
    val patterns = spec["pattern"].toStringList()
    if (patterns.isEmpty()) return
    val regionType = getRegionType(spec)

    // A single "pattern" is wired to "select"; a [header, footer] pair is wired to "select-a" / "select-i".
    registerSelect(plugin, obj, "select", spec["select"], patterns, isInner = false, regionType)
    registerSelect(plugin, obj, "select-a", spec["select-a"], patterns, isInner = false, regionType)
    registerSelect(plugin, obj, "select-i", spec["select-i"], patterns, isInner = true, regionType)

    // "move-*" jump to the beginning ("n"/"p") or end ("N"/"P") of the next/previous object.
    registerMotion(plugin, obj, "move-n", spec["move-n"], patterns, Motion.NEXT_START)
    registerMotion(plugin, obj, "move-p", spec["move-p"], patterns, Motion.PREVIOUS_START)
    registerMotion(plugin, obj, "move-N", spec["move-N"], patterns, Motion.NEXT_END)
    registerMotion(plugin, obj, "move-P", spec["move-P"], patterns, Motion.PREVIOUS_END)
  }

  private fun registerSelect(
    plugin: String,
    obj: String,
    specName: String,
    keys: VimDataType?,
    patterns: List<String>,
    isInner: Boolean,
    regionType: SelectionType,
  ) {
    val userKeys = keys.toStringList()
    if (userKeys.isEmpty()) return
    // Text objects apply in Visual and Operator-pending modes.
    val plug = plugName(plugin, obj, specName)
    putExtensionHandlerMapping(
      MappingMode.XO,
      injector.parser.parseKeys(plug),
      owner,
      TextObjUserHandler(patterns, isInner, regionType),
      false,
    )
    mapUserKeys(owner, userKeys, plug, MappingMode.XO)
  }

  private fun registerMotion(
    plugin: String,
    obj: String,
    specName: String,
    keys: VimDataType?,
    patterns: List<String>,
    motion: Motion,
  ) {
    val userKeys = keys.toStringList()
    if (userKeys.isEmpty()) return
    // Motions apply in Normal, Visual and Operator-pending modes.
    val plug = plugName(plugin, obj, specName)
    putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys(plug),
      owner,
      TextObjUserMotionHandler(patterns.first(), motion),
      false,
    )
    mapUserKeys(owner, userKeys, plug, MappingMode.NXO)
  }

  private fun getRegionType(spec: VimDictionary): SelectionType {
    val regionType = spec["region-type"] as? VimString ?: return SelectionType.CHARACTER_WISE
    return when (regionType.value) {
      "V" -> SelectionType.LINE_WISE
      "\u0016" -> SelectionType.BLOCK_WISE // CTRL-V, i.e. "\<C-v>"
      else -> SelectionType.CHARACTER_WISE
    }
  }
}

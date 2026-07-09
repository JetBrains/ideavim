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
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

/**
 * Handles `textobj#user#plugin({name}, {specs})` by registering a text object mapping for every `select` / `move-*`
 * key in the given specs.
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
    val specs = arguments[1].evaluate(editor, context, vimContext) as VimDictionary
    for ((_, spec) in specs.dictionary) {
      registerTextObject(spec as VimDictionary)
    }
    return VimString("")
  }

  private fun registerTextObject(spec: VimDictionary) {
    // "pattern" is either a single Vim regex, or a [header, footer] pair of Vim regexes.
    val patterns = spec["pattern"].toStringList()
    if (patterns.isEmpty()) return
    val regionType = getRegionType(spec)

    // A single "pattern" is wired to "select"; a [header, footer] pair is wired to "select-a" / "select-i".
    registerMappings(spec["select"], patterns, isInner = false, regionType = regionType)
    registerMappings(spec["select-a"], patterns, isInner = false, regionType = regionType)
    registerMappings(spec["select-i"], patterns, isInner = true, regionType = regionType)

    // "move-*" jump to the beginning ("n"/"p") or end ("N"/"P") of the next/previous object.
    registerMotions(spec["move-n"], patterns, Motion.NEXT_START)
    registerMotions(spec["move-p"], patterns, Motion.PREVIOUS_START)
    registerMotions(spec["move-N"], patterns, Motion.NEXT_END)
    registerMotions(spec["move-P"], patterns, Motion.PREVIOUS_END)
  }

  private fun registerMappings(
    keys: VimDataType?,
    patterns: List<String>,
    isInner: Boolean,
    regionType: TextObjectVisualType,
  ) {
    for (key in keys.toStringList()) {
      putExtensionHandlerMapping(
        MappingMode.XO,
        injector.parser.parseKeys(key),
        owner,
        TextObjUserHandler(patterns, isInner, regionType),
        false,
      )
    }
  }

  private fun registerMotions(keys: VimDataType?, patterns: List<String>, motion: Motion) {
    for (key in keys.toStringList()) {
      putExtensionHandlerMapping(
        MappingMode.NXO,
        injector.parser.parseKeys(key),
        owner,
        TextObjUserMotionHandler(patterns.first(), motion),
        false,
      )
    }
  }

  private fun getRegionType(spec: VimDictionary): TextObjectVisualType {
    val regionType = spec["region-type"] as? VimString ?: return TextObjectVisualType.CHARACTER_WISE
    return when (regionType.value) {
      "V" -> TextObjectVisualType.LINE_WISE
      else -> TextObjectVisualType.CHARACTER_WISE
    }
  }
}

/**
 * Flattens a vim-textobj-user field that accepts either a single string or a list of strings (`pattern`, `select`, ...)
 * into a list. Returns an empty list when the field is absent.
 */
private fun VimDataType?.toStringList(): List<String> = when (this) {
  is VimString -> listOf(value)
  is VimList -> values.map { (it as VimString).value }
  else -> emptyList()
}

/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.datatypes

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.VimTestCase

abstract class VimDataTypeTest : VimTestCase() {
  protected fun toVimDataType(value: Any) = when (value) {
    is Int -> VimInt(value)
    is Double -> VimFloat(value)
    is String -> VimString(value)
    is List<*> -> toVimList(value)
    is Map<*, *> -> toVimDictionary(value)
    is VimDataType -> value
    else -> error("Unsupported type: ${value::class.simpleName}")
  }

  private fun toVimList(list: List<*>): VimList =
    VimList(list.map { toVimDataType(it!!) }.toMutableList())

  protected fun toVimList(vararg elements: Any): VimList =
    toVimList(elements.toList())

  private fun toVimDictionary(dictionary: Map<*, *>): VimDictionary {
    val map = LinkedHashMap<VimString, VimDataType>()
    for ((k, v) in dictionary) {
      map[VimString(k as String)] = toVimDataType(v!!)
    }
    return VimDictionary(map)
  }

  protected fun toVimDictionary(vararg elements: Pair<String, Any>): VimDictionary =
    toVimDictionary(elements.toMap())
}

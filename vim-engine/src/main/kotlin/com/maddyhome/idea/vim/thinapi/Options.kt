/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.Option
import com.maddyhome.idea.vim.options.Option as EngineOption
import com.intellij.vim.api.OptionType
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.options.OptionDeclaredScope as EngineOptionDeclaredScope

internal fun <T : OptionType> EngineOption<*>.toApiOption(): Option<T>? {
  return try {
    when (this) {
      is StringOption -> {
        val defaultValue = OptionType.StringType(defaultValue.asString())
        val unsetValue = OptionType.StringType(unsetValue.asString())
        Option.StringOption(
          name,
          declaredScope.toApiDeclaredScope(),
          abbrev,
          defaultValue,
          unsetValue,
          isLocalNoGlobal,
          isHidden
        ) as Option<OptionType.StringType>
      }

      is StringListOption -> {
        val defaultValue = OptionType.StringType(defaultValue.asString())
        val unsetValue = OptionType.StringType(unsetValue.asString())
        Option.StringListOption(
          name,
          declaredScope.toApiDeclaredScope(),
          abbrev,
          defaultValue,
          unsetValue,
          isLocalNoGlobal,
          isHidden,
          boundedValues?.toList()
        ) as Option<OptionType.StringType>
      }

      is NumberOption -> {
        val defaultValue = OptionType.IntType(defaultValue.value)
        val unsetValue = OptionType.IntType(unsetValue.value)
        Option.IntOption(
          name,
          declaredScope.toApiDeclaredScope(),
          abbrev,
          defaultValue,
          unsetValue,
          isLocalNoGlobal,
          isHidden
        ) as Option<OptionType.IntType>
      }

      is ToggleOption -> {
        val defaultValue = OptionType.BooleanType(defaultValue.asBoolean())
        val unsetValue = OptionType.BooleanType(unsetValue.asBoolean())
        Option.BooleanOption(
          name,
          declaredScope.toApiDeclaredScope(),
          abbrev,
          defaultValue,
          unsetValue,
          isLocalNoGlobal,
          isHidden
        ) as Option<OptionType.BooleanType>
      }

      else -> null
    }
  } catch (e: Exception) {
    null
  } as Option<T>?
}

fun EngineOptionDeclaredScope.toApiDeclaredScope(): com.intellij.vim.api.OptionDeclaredScope {
  return when (this) {
    EngineOptionDeclaredScope.GLOBAL -> com.intellij.vim.api.OptionDeclaredScope.GLOBAL
    EngineOptionDeclaredScope.LOCAL_TO_BUFFER -> com.intellij.vim.api.OptionDeclaredScope.LOCAL_TO_BUFFER
    EngineOptionDeclaredScope.LOCAL_TO_WINDOW -> com.intellij.vim.api.OptionDeclaredScope.LOCAL_TO_WINDOW
    EngineOptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER -> com.intellij.vim.api.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
    EngineOptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW -> com.intellij.vim.api.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW
  }
}
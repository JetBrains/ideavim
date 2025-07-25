/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.OptionScope
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import kotlin.reflect.KType

class OptionScopeImpl: OptionScope() {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val optionGroup: VimOptionGroup
    get() = injector.optionGroup

  override fun <T> getOptionValueInternal(name: String, type: KType): T? {
    val option = optionGroup.getOption(name) ?: return null

    val optionValue = optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(vimEditor))

    val kotlinType = type.classifier ?: return null
    val kotlinValue = when (optionValue) {
      is VimInt -> {
        val intValue = optionValue.value
        when(kotlinType) {
          Int::class -> intValue
          Boolean::class -> intValue == VimInt.ONE.value

          else -> {
            throw IllegalArgumentException("Wrong option type. Expected boolean or integer, got $kotlinType. Option name: $name, value: $intValue.")
          }
        }
      }

      is VimString -> {
        val stringValue = optionValue.value
        when(kotlinType) {
          String::class -> optionValue.value

          else -> {
            throw IllegalArgumentException("Wrong option type. Expected string, got $kotlinType. Option name: $name, value: $stringValue")
          }
        }
      }

      else -> {
        throw IllegalArgumentException("Options can only be of types string, integer and boolean.")
      }
    }

    return kotlinValue as T?
  }

  override fun <T> setOptionInternal(name: String, value: T, type: KType, scope: String): Boolean {
    val option = optionGroup.getOption(name) ?: return false

    val optionValue = when (type.classifier) {
      Int::class -> {
        VimInt(value as Int)
      }

      String::class -> {
        VimString(value as String)
      }

      Boolean::class -> {
        if (value as Boolean) VimInt.ONE else VimInt.ZERO
      }

      else -> {
        throw IllegalArgumentException("Options can only be of types string, integer and boolean")
      }
    }
    val optionAccessScope = when (scope) {
      "global" -> OptionAccessScope.GLOBAL(vimEditor)
      "local" -> OptionAccessScope.LOCAL(vimEditor)
      "effective" -> OptionAccessScope.EFFECTIVE(vimEditor)
      else -> OptionAccessScope.EFFECTIVE(vimEditor)
    }
    optionGroup.setOptionValue(option, optionAccessScope, optionValue)
    return true
  }

  override fun reset(name: String): Boolean {
    val option = optionGroup.getOption(name) ?: return false
    optionGroup.resetToDefaultValue(option, OptionAccessScope.EFFECTIVE(vimEditor))
    return true
  }
}
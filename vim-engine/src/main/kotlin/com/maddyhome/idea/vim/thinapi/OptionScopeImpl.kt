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
    return injector.variableService.convertToKotlinType(optionValue, type)
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

      else -> return false
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

  override fun resetOptionToDefault(name: String): Boolean {
    val option = optionGroup.getOption(name) ?: return false
    optionGroup.resetToDefaultValue(option, OptionAccessScope.EFFECTIVE(vimEditor))
    return true
  }
}
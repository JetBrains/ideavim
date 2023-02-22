/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.appendValue
import com.maddyhome.idea.vim.options.removeValue
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

object ClipboardOptionHelper {
  var ideaputDisabled = false
    private set

  class IdeaputDisabler : AutoCloseable {
    private val containedBefore = injector.globalOptions().hasValue(OptionConstants.clipboard, OptionConstants.clipboard_ideaput)

    init {
      injector.optionGroup.getOption(OptionConstants.clipboard)?.let { option ->
        val value = injector.optionGroup.getOptionValue(option, OptionScope.GLOBAL)
        option.removeValue(value, VimString(OptionConstants.clipboard_ideaput))?.let {
          injector.optionGroup.setOptionValue(option, OptionScope.GLOBAL, it)
        }
      }
      ideaputDisabled = true
    }

    override fun close() {
      if (containedBefore) {
        injector.optionGroup.getOption(OptionConstants.clipboard)?.let { option ->
          val value = injector.optionGroup.getOptionValue(option, OptionScope.GLOBAL)
          option.appendValue(value, VimString(OptionConstants.clipboard_ideaput))?.let {
            injector.optionGroup.setOptionValue(option, OptionScope.GLOBAL, it)
          }
        }
      }
      ideaputDisabled = false
    }
  }
}

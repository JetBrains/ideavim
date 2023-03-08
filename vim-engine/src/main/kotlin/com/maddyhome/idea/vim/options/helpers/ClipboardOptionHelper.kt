/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.getKnownStringOption
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.modifyOptionValue
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

object ClipboardOptionHelper {
  var ideaputDisabled = false
    private set

  class IdeaputDisabler : AutoCloseable {
    private val containedBefore =
      injector.globalOptions().hasValue(OptionConstants.clipboard, OptionConstants.clipboard_ideaput)

    init {
      modifyClipboardOption { option, currentValue ->
        option.removeValue(currentValue, VimString(OptionConstants.clipboard_ideaput))
      }

      ideaputDisabled = true
    }

    override fun close() {
      if (containedBefore) {
        modifyClipboardOption { option, currentValue ->
          option.appendValue(currentValue, VimString(OptionConstants.clipboard_ideaput))
        }
      }
      ideaputDisabled = false
    }

    private inline fun modifyClipboardOption(transform: (StringOption, VimString) -> VimString) {
      val option = injector.optionGroup.getKnownStringOption(OptionConstants.clipboard)
      injector.optionGroup.modifyOptionValue(option, OptionScope.GLOBAL) {
        transform(option, it)
      }
    }
  }
}

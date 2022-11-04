/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

object ClipboardOptionHelper {
  var ideaputDisabled = false
    private set

  class IdeaputDisabler : AutoCloseable {
    private val containedBefore: Boolean

    init {
      val optionValue = (injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.clipboardName) as VimString).value
      containedBefore = optionValue.contains(OptionConstants.clipboard_ideaput)
      injector.optionService.removeValue(
        OptionScope.GLOBAL,
        OptionConstants.clipboardName,
        OptionConstants.clipboard_ideaput,
        OptionConstants.clipboardName
      )
      ideaputDisabled = true
    }

    override fun close() {
      if (containedBefore) injector.optionService.appendValue(
        OptionScope.GLOBAL,
        OptionConstants.clipboardName,
        OptionConstants.clipboard_ideaput,
        OptionConstants.clipboardName
      )
      ideaputDisabled = false
    }
  }
}

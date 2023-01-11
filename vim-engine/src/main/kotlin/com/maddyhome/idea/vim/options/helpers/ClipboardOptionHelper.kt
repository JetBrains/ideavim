/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope

object ClipboardOptionHelper {
  var ideaputDisabled = false
    private set

  class IdeaputDisabler : AutoCloseable {
    private val containedBefore = injector.globalOptions().hasValue(OptionConstants.clipboard, OptionConstants.clipboard_ideaput)

    init {
      injector.optionService.removeValue(
        OptionScope.GLOBAL,
        OptionConstants.clipboard,
        OptionConstants.clipboard_ideaput,
        OptionConstants.clipboard
      )
      ideaputDisabled = true
    }

    override fun close() {
      if (containedBefore) {
        injector.optionService.appendValue(
          OptionScope.GLOBAL,
          OptionConstants.clipboard,
          OptionConstants.clipboard_ideaput
        )
      }
      ideaputDisabled = false
    }
  }
}

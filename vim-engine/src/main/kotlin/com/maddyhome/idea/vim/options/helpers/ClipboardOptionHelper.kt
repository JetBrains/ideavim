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
import com.maddyhome.idea.vim.api.unsafeAppendGlobalKnownOptionValue
import com.maddyhome.idea.vim.api.unsafeRemoveGlobalKnownOptionValue
import com.maddyhome.idea.vim.options.OptionConstants

object ClipboardOptionHelper {
  var ideaputDisabled = false
    private set

  class IdeaputDisabler : AutoCloseable {
    private val containedBefore = injector.globalOptions().hasValue(OptionConstants.clipboard, OptionConstants.clipboard_ideaput)

    init {
      injector.optionGroup.unsafeRemoveGlobalKnownOptionValue(
        OptionConstants.clipboard,
        OptionConstants.clipboard_ideaput
      )
      ideaputDisabled = true
    }

    override fun close() {
      if (containedBefore) {
        injector.optionGroup.unsafeAppendGlobalKnownOptionValue(
          OptionConstants.clipboard,
          OptionConstants.clipboard_ideaput
        )
      }
      ideaputDisabled = false
    }
  }
}

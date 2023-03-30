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

public object ClipboardOptionHelper {
  public var ideaputDisabled: Boolean = false
    private set

  public class IdeaputDisabler : AutoCloseable {
    private val containedBefore =
      injector.globalOptions().clipboard.contains(OptionConstants.clipboard_ideaput)

    init {
      injector.globalOptions().clipboard.removeValue(OptionConstants.clipboard_ideaput)
      ideaputDisabled = true
    }

    override fun close() {
      if (containedBefore) {
        injector.globalOptions().clipboard.prependValue(OptionConstants.clipboard_ideaput)
      }
      ideaputDisabled = false
    }
  }
}

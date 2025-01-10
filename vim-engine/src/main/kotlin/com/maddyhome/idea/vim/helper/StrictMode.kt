/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger

object StrictMode {
  private val LOG = vimLogger<StrictMode>()

  @JvmName("assertTrue")
  fun assert(condition: Boolean, message: String) {
    if (!condition) {
      fail(message)
    }
  }

  fun fail(message: String) {
    if (injector.globalOptions().ideastrictmode) {
      error(message)
    } else {
      LOG.error(message)
    }
  }
}

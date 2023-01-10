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

object StrictMode {
  @JvmName("assertTrue")
  fun assert(condition: Boolean, message: String) {
    if (!condition) {
      fail(message)
    }
  }

  fun fail(message: String) {
    if (injector.optionService.isSet(OptionScope.GLOBAL, OptionConstants.ideastrictmodeName)) {
      error(message)
    }
  }
}

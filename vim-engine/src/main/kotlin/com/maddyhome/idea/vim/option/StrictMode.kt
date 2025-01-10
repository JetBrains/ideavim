/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector

@Deprecated(
  "Please use StrictMode from com.maddyhome.idea.vim.options.helpers",
  replaceWith = ReplaceWith("com.maddyhome.idea.vim.helper.StrictMode")
)
object StrictMode {
  @JvmName("assertTrue")
  fun assert(condition: Boolean, message: String) {
    if (!condition) {
      fail(message)
    }
  }

  fun fail(message: String) {
    if (injector.globalOptions().ideastrictmode) {
      error(message)
    }
  }
}

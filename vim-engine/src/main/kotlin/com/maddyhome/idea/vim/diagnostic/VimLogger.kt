/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.diagnostic

import com.maddyhome.idea.vim.api.injector

interface VimLogger {
  fun isTrace(): Boolean
  fun trace(data: String)

  fun isDebug(): Boolean
  fun debug(data: String)

  fun warn(message: String)
  fun error(message: String)
  fun info(message: String)
}

fun VimLogger.trace(message: () -> String) {
  if (isTrace()) {
    trace(message())
  }
}

fun VimLogger.debug(message: () -> String) {
  if (isDebug()) {
    debug(message())
  }
}

inline fun <reified T : Any> vimLogger(): VimLogger = injector.getLogger(T::class.java)

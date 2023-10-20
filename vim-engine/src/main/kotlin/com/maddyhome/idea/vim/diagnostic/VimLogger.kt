/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.diagnostic

import com.maddyhome.idea.vim.api.injector

public interface VimLogger {
  public fun isTrace(): Boolean
  public fun trace(data: String)

  public fun isDebug(): Boolean
  public fun debug(data: String)

  public fun warn(message: String)
  public fun error(message: String)
  public fun error(message: String, e: Throwable)
  public fun info(message: String)
}

public fun VimLogger.trace(message: () -> String) {
  if (isTrace()) {
    trace(message())
  }
}

public fun VimLogger.debug(message: () -> String) {
  if (isDebug()) {
    debug(message())
  }
}

public inline fun <reified T : Any> vimLogger(): VimLogger = injector.getLogger(T::class.java)

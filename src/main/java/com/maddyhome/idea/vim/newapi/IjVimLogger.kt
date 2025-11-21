/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.diagnostic.Logger
import com.maddyhome.idea.vim.diagnostic.VimLogger

internal class IjVimLogger(private val logger: Logger) : VimLogger {
  override fun isTrace(): Boolean = logger.isTraceEnabled

  override fun trace(data: String) {
    logger.trace(data)
  }

  override fun isDebug(): Boolean = logger.isDebugEnabled

  override fun debug(data: String) {
    logger.debug(data)
  }

  override fun warn(message: String) {
    logger.warn(message)
  }

  override fun warn(message: String, e: Throwable) {
    logger.warn(message, e)
  }

  override fun error(message: String) {
    logger.error(message)
  }

  override fun error(message: String, e: Throwable) {
    logger.error(message, e)
  }

  override fun info(message: String) {
    logger.info(message)
  }
}

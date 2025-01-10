/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser.errors

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class IdeavimErrorListener : BaseErrorListener() {
  private val logger = vimLogger<IdeavimErrorListener>()

  companion object {
    val testLogger: MutableList<String> = mutableListOf<String>()
  }

  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String?,
    e: RecognitionException?,
  ) {
    injector.vimscriptParser.linesWithErrors.add(line)
    val message = "line $line:$charPositionInLine $msg"
    if (injector.application.isUnitTest()) {
      testLogger.add(message)
    } else {
      logger.warn(message)
    }
  }
}

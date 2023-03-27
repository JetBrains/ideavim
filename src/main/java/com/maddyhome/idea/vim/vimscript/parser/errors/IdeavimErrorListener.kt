/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser.errors

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

internal class IdeavimErrorListener : BaseErrorListener() {
  private val logger = logger<IdeavimErrorListener>()

  companion object {
    val testLogger = mutableListOf<String>()
  }

  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String?,
    e: RecognitionException?,
  ) {
    VimscriptParser.linesWithErrors.add(line)
    val message = "line $line:$charPositionInLine $msg"
    if (ApplicationManager.getApplication().isUnitTestMode) {
      testLogger.add(message)
    } else {
      logger.warn(message)
    }
  }
}

/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.parser.errors

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class IdeavimErrorListener : BaseErrorListener() {
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
    e: RecognitionException?
  ) {
    val message = "line $line:$charPositionInLine $msg"
    if (ApplicationManager.getApplication().isUnitTestMode) {
      testLogger.add(message)
    } else {
      logger.warn(message)
    }
  }
}

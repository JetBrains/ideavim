/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import org.jetbrains.annotations.PropertyKey

class InvalidCommandException(message: String, cmd: String?) : ExException(message + if (cmd != null) " | $cmd" else "")

class InvalidRangeException(s: String) : ExException(s)

class MissingRangeException : ExException()

class NoArgumentAllowedException : ExException()

class FinishException : ExException()

fun exExceptionMessage(@PropertyKey(resourceBundle = EngineMessageHelper.BUNDLE) code: String, vararg params: Any): ExException =
  ExException(injector.messages.message(code, *params)).apply { this.code = code }

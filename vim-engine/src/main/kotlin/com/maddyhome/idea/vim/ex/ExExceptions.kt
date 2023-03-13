/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex

import com.maddyhome.idea.vim.api.injector

public class InvalidCommandException(message: String, cmd: String?) : ExException(message + if (cmd != null) " | $cmd" else "")

public class InvalidRangeException(s: String) : ExException(s)

public class MissingArgumentException : ExException()

public class MissingRangeException : ExException()

public class NoArgumentAllowedException : ExException()

public class NoRangeAllowedException : ExException()

public class FinishException : ExException()

public fun exExceptionMessage(code: String, vararg params: Any): ExException =
  ExException(injector.messages.message(code, *params)).apply { this.code = code }

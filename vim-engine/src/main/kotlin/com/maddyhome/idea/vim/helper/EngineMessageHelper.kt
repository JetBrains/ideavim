/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import org.jetbrains.annotations.PropertyKey
import java.text.MessageFormat
import java.util.*

object EngineMessageHelper {
  internal const val BUNDLE = "messages.IdeaVimEngineBundle"

  private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE, ::javaClass.get().module) }

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String): String = bundle.getString(key)

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
    if (params.isEmpty()) bundle.getString(key) else MessageFormat.format(bundle.getString(key), *params)
}

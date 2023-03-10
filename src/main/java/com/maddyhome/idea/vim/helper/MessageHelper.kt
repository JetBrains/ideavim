/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val IDEAVIM_BUNDLE = "messages.IdeaVimBundle"

internal object MessageHelper : DynamicBundle(IDEAVIM_BUNDLE) {

  private const val BUNDLE = IDEAVIM_BUNDLE

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String) = getMessage(key)
}

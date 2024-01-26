/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.injector
import java.util.*
import java.util.stream.Collectors
import javax.swing.KeyStroke

/**
 * COMPATIBILITY-LAYER: Created a helper class
 * Please see: https://jb.gg/zo8n0r
 */
public object StringHelper {
  @JvmStatic
  @Deprecated("Use injector.parser.parseKeys(string)",
    ReplaceWith("injector.parser.parseKeys(string)", "com.maddyhome.idea.vim.api.injector")
  )
  public fun parseKeys(string: String): List<KeyStroke> {
    return injector.parser.parseKeys(string)
  }

  @JvmStatic
  @Deprecated("Use injector.parser.parseKeys(string)")
  public fun parseKeys(vararg string: String): List<KeyStroke> {
    return Arrays.stream(string).flatMap { o: String -> injector.parser.parseKeys(o).stream() }
      .collect(Collectors.toList())
  }
}

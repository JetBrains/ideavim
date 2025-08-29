/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.stream.Collectors
import javax.swing.KeyStroke

object StringHelper {
  @JvmStatic
  @Deprecated("Use injector.parser.parseKeys(string)")
  @ApiStatus.ScheduledForRemoval
  fun parseKeys(vararg string: String): List<KeyStroke> {
    return Arrays.stream(string).flatMap { o: String -> injector.parser.parseKeys(o).stream() }
      .collect(Collectors.toList())
  }

  /**
   * Returns the character at the specified index in the string.
   * Test method for GitHub integration.
   */
  @JvmStatic
  fun getCharAtIndex(str: String, index: Int): Char? {
    if (index < 0 || index > str.length) {
      return null
    }
    return str[index]
  }
}

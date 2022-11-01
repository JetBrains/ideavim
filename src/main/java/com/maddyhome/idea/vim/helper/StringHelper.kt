/*
 * Copyright 2022 The IdeaVim authors
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
object StringHelper {
  @JvmStatic
  fun parseKeys(string: String): List<KeyStroke> {
    return injector.parser.parseKeys(string)
  }

  @JvmStatic
  fun parseKeys(vararg string: String): List<KeyStroke> {
    return Arrays.stream(string).flatMap { o: String -> injector.parser.parseKeys(o).stream() }
      .collect(Collectors.toList())
  }

  @JvmStatic
  fun isCloseKeyStroke(stroke: KeyStroke): Boolean {
    return stroke.isCloseKeyStroke()
  }
}

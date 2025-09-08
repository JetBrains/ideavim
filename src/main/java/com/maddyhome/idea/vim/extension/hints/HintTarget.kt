/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import javax.accessibility.Accessible

internal data class HintTarget(val component: Accessible, val location: Point, val size: Dimension, val depth: Int) {
  var hint: String = ""

  val bounds: Rectangle get() = Rectangle(location, size)
}

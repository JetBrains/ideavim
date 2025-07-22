/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

data class Color(
  val hexCode: String
) {
  constructor(r: Int, g: Int, b: Int, a: Int = 255): this(String.format("#%02x%02x%02x%02x", r, g, b, a))

  val r: Int = hexCode.substring(1..2).toInt(16)

  val g: Int = hexCode.substring(3..4).toInt(16)

  val b: Int = hexCode.substring(5..6).toInt(16)

  val a: Int = if (hexCode.length == 9) hexCode.substring(7..8).toInt(16) else 255

  init {
    require(hexCode.matches(Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$"))) {
      "Hex code should be in format #RRGGBB[AA]"
    }
  }
}
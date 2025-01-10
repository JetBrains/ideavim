/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.regexp

@Deprecated("Remove once old regex engine is removed")
object Magic {
  const val AMP: Int = '&'.code - 256
  const val AT: Int = '@'.code - 256
  const val DOLLAR: Int = '$'.code - 256
  const val DOT: Int = '.'.code - 256
  const val EQUAL: Int = '='.code - 256
  const val GREATER: Int = '>'.code - 256
  const val HAT: Int = '^'.code - 256
  const val LBRACE: Int = '['.code - 256
  const val LCURLY: Int = '{'.code - 256
  const val LESS: Int = '<'.code - 256
  const val LPAREN: Int = '('.code - 256
  const val PERCENT: Int = '%'.code - 256
  const val PIPE: Int = '|'.code - 256
  const val PLUS: Int = '+'.code - 256
  const val QUESTION: Int = '?'.code - 256
  const val RPAREN: Int = ')'.code - 256
  const val STAR: Int = '*'.code - 256
  const val TILDE: Int = '~'.code - 256
  const val UNDER: Int = '_'.code - 256
  const val N0: Int = '0'.code - 256
  const val N1: Int = '1'.code - 256
  const val N2: Int = '2'.code - 256
  const val N3: Int = '3'.code - 256
  const val N4: Int = '4'.code - 256
  const val N5: Int = '5'.code - 256
  const val N6: Int = '6'.code - 256
  const val N7: Int = '7'.code - 256
  const val N8: Int = '8'.code - 256
  const val N9: Int = '9'.code - 256
  const val a: Int = 'a'.code - 256
  const val A: Int = 'A'.code - 256
  const val c: Int = 'c'.code - 256
  const val C: Int = 'C'.code - 256
  const val d: Int = 'd'.code - 256
  const val D: Int = 'D'.code - 256
  const val f: Int = 'f'.code - 256
  const val F: Int = 'F'.code - 256
  const val h: Int = 'h'.code - 256
  const val H: Int = 'H'.code - 256
  const val i: Int = 'i'.code - 256
  const val I: Int = 'I'.code - 256
  const val k: Int = 'k'.code - 256
  const val K: Int = 'K'.code - 256
  const val l: Int = 'l'.code - 256
  const val L: Int = 'L'.code - 256
  const val m: Int = 'm'.code - 256
  const val M: Int = 'M'.code - 256
  const val n: Int = 'n'.code - 256
  const val N: Int = 'N'.code - 256
  const val o: Int = 'o'.code - 256
  const val O: Int = 'O'.code - 256
  const val p: Int = 'p'.code - 256
  const val P: Int = 'P'.code - 256
  const val s: Int = 's'.code - 256
  const val S: Int = 'S'.code - 256
  const val u: Int = 'u'.code - 256
  const val U: Int = 'U'.code - 256
  const val v: Int = 'v'.code - 256
  const val V: Int = 'V'.code - 256
  const val w: Int = 'w'.code - 256
  const val W: Int = 'W'.code - 256
  const val x: Int = 'x'.code - 256
  const val X: Int = 'X'.code - 256
  const val z: Int = 'z'.code - 256

  /*
 * Magic characters have a special meaning, they don't match literally.
 * Magic characters are negative.  This separates them from literal characters
 * (possibly multi-byte).  Only ASCII characters can be Magic.
 */
  fun magic(x: Int): Int {
    return x - 256
  }

  fun un_Magic(x: Int): Int {
    return x + 256
  }

  fun is_Magic(x: Int): Boolean {
    return x < 0
  }

  fun no_Magic(x: Int): Int {
    return if (is_Magic(x)) {
      un_Magic(x)
    } else {
      x
    }
  }

  fun toggle_Magic(x: Int): Int {
    return if (is_Magic(x)) {
      un_Magic(x)
    } else {
      magic(x)
    }
  }
}

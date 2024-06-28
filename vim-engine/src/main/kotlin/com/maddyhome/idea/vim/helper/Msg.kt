/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

interface Msg {
  companion object {
    const val NOT_EX_CMD: String = "notexcmd"
    const val INT_BAD_CMD: String = "intbadcmd"
    const val e_backslash: String = "e_backslash"
    const val e_badrange: String = "e_badrange"
    const val e_rangereq: String = "e_rangereq"
    const val e_argforb: String = "e_argforb"
    const val e_noprev: String = "e_noprev"
    const val e_nopresub: String = "e_nopresub"
    const val E191: String = "E191"
    const val e_backrange: String = "e_backrange"
    const val E146: String = "E146"
    const val e_zerocount: String = "e_zerocount"
    const val e_trailing: String = "e_trailing"
    const val e_invcmd: String = "e_invcmd"
    const val e_null: String = "e_null"
    const val E50: String = "E50"
    const val E51: String = "E51"
    const val E52: String = "E52"
    const val E53: String = "E53"
    const val E54: String = "E54"
    const val E55: String = "E55"
    const val E56: String = "E56"
    const val E57: String = "E57"
    const val E58: String = "E58"
    const val E59: String = "E59"
    const val E60: String = "E60"
    const val E61: String = "E61"
    const val E62: String = "E62"
    const val E63: String = "E63"
    const val E64: String = "E64"
    const val E65: String = "E65"
    const val E66: String = "E66"
    const val E67: String = "E67"
    const val E68: String = "E68"
    const val E69: String = "E69"
    const val E70: String = "E70"
    const val E71: String = "E71"
    const val e_invrange: String = "e_invrange"
    const val e_toomsbra: String = "e_toomsbra"
    const val e_internal: String = "e_internal"
    const val synerror: String = "synerror"
    const val E363: String = "E363"
    const val e_re_corr: String = "e_re_corr"
    const val e_re_damg: String = "e_re_damg"
    const val E369: String = "E369"
    const val E384: String = "E384"
    const val E385: String = "E385"
    const val unkopt: String = "unkopt"
    const val e_invarg: String = "e_invarg"
    const val E475: String = "E475"
  }
}

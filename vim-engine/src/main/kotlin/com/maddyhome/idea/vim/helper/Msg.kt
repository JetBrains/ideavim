/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.helper

interface Msg {
  companion object {
    const val NOT_EX_CMD = "notexcmd"
    const val INT_BAD_CMD = "intbadcmd"
    const val e_backslash = "e_backslash"
    const val e_badrange = "e_badrange"
    const val e_norange = "e_norange"
    const val e_rangereq = "e_rangereq"
    const val e_argreq = "e_argreq"
    const val e_argforb = "e_argforb"
    const val e_noprev = "e_noprev"
    const val e_nopresub = "e_nopresub"
    const val E191 = "E191"
    const val e_backrange = "e_backrange"
    const val E146 = "E146"
    const val e_zerocount = "e_zerocount"
    const val e_trailing = "e_trailing"
    const val e_invcmd = "e_invcmd"
    const val e_null = "e_null"
    const val E50 = "E50"
    const val E51 = "E51"
    const val E52 = "E52"
    const val E53 = "E53"
    const val E54 = "E54"
    const val E55 = "E55"
    const val E56 = "E56"
    const val E57 = "E57"
    const val E58 = "E58"
    const val E59 = "E59"
    const val E60 = "E60"
    const val E61 = "E61"
    const val E62 = "E62"
    const val E63 = "E63"
    const val E64 = "E64"
    const val E65 = "E65"
    const val E66 = "E66"
    const val E67 = "E67"
    const val E68 = "E68"
    const val E69 = "E69"
    const val E70 = "E70"
    const val E71 = "E71"
    const val e_invrange = "e_invrange"
    const val e_toomsbra = "e_toomsbra"
    const val e_internal = "e_internal"
    const val synerror = "synerror"
    const val E363 = "E363"
    const val e_re_corr = "e_re_corr"
    const val e_re_damg = "e_re_damg"
    const val E369 = "E369"
    const val E384 = "E384"
    const val E385 = "E385"
    const val e_patnotf2 = "e_patnotf2"
    const val unkopt = "unkopt"
    const val e_invarg = "e_invarg"
    const val E475 = "E475"
  }
}

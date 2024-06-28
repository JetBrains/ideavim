/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.state.mode

/**
 * Get the selection type if the mode is [Mode.VISUAL] or [Mode.SELECT]. Otherwise, returns null.
 *
 * Note that if the mode is [Mode.CMD_LINE], we return the selection type of the underlying editor. This only has an
 * effect for (inc)search, as we switch to [Mode.NORMAL] before entering an ex command.
 */
@Suppress("RecursivePropertyAccessor")
val Mode.selectionType: SelectionType?
  get() = when (this) {
    is Mode.VISUAL -> this.selectionType
    is Mode.SELECT -> this.selectionType
    is Mode.CMD_LINE -> this.returnTo().selectionType
    else -> null
  }

/**
 * Get the mode that we need to return to if the one-command-mode (':h i_Ctrl-o') is active.
 * Otherwise, returns null.
 */
val Mode.returnTo: ReturnTo?
  get() = when (this) {
    is Mode.NORMAL -> this.returnTo
    is Mode.SELECT -> this.returnTo
    is Mode.VISUAL -> this.returnTo
    is Mode.OP_PENDING -> this.returnTo
    else -> null
  }

/**
 * Check if one-command-mode (':h i_Ctrl-o') is active.
 */
val Mode.isSingleModeActive: Boolean
  get() = returnTo != null

/**
 * Check if the caret can be placed after the end of line.
 *
 * `onemore` option is ignored.
 */
val Mode.isEndAllowedIgnoringOnemore: Boolean
  get() = when (this) {
    is Mode.INSERT, is Mode.VISUAL, is Mode.SELECT -> true
    else -> false
  }

val SelectionType.isLine: Boolean get() = this == SelectionType.LINE_WISE
val SelectionType.isChar: Boolean get() = this == SelectionType.CHARACTER_WISE
val SelectionType.isBlock: Boolean get() = this == SelectionType.BLOCK_WISE

/**
 * Convert the IdeaVim [Mode] into a string according to the rules of `mode()` function in Vim.
 *
 *  Neovim
 * :h mode()
 *
 * - mode(expr)          Return a string that indicates the current mode.
 *
 *   If "expr" is supplied and it evaluates to a non-zero Number or
 *   a non-empty String (|non-zero-arg|), then the full mode is
 *   returned, otherwise only the first letter is returned.
 *
 *   n          Normal
 *   no         Operator-pending
 *   nov        Operator-pending (forced characterwise |o_v|)
 *   noV        Operator-pending (forced linewise |o_V|)
 *   noCTRL-V   Operator-pending (forced blockwise |o_CTRL-V|)
 *   niI        Normal using |i_CTRL-O| in |Insert-mode|
 *   niR        Normal using |i_CTRL-O| in |Replace-mode|
 *   niV        Normal using |i_CTRL-O| in |Virtual-Replace-mode|
 *   v          Visual by character
 *   V          Visual by line
 *   CTRL-V     Visual blockwise
 *   s          Select by character
 *   S          Select by line
 *   CTRL-S     Select blockwise
 *   i          Insert
 *   ic         Insert mode completion |compl-generic|
 *   ix         Insert mode |i_CTRL-X| completion
 *   R          Replace |R|
 *   Rc         Replace mode completion |compl-generic|
 *   Rv         Virtual Replace |gR|
 *   Rx         Replace mode |i_CTRL-X| completion
 *   c          Command-line editing
 *   cv         Vim Ex mode |gQ|
 *   ce         Normal Ex mode |Q|
 *   r          Hit-enter prompt
 *   rm         The -- more -- prompt
 *   r?         |:confirm| query of some sort
 *   !          Shell or external command is executing
 *   t          Terminal mode: keys go to the job
 *   This is useful in the 'statusline' option or when used
 *   with |remote_expr()| In most other places it always returns
 *   "c" or "n".
 *   Note that in the future more modes and more specific modes may
 *   be added. It's better not to compare the whole string but only
 *   the leading character(s).
 */
fun Mode.toVimNotation(): String {
  return when (this) {
    is Mode.NORMAL -> "n"
    is Mode.VISUAL -> when (selectionType) {
      SelectionType.CHARACTER_WISE -> "v"
      SelectionType.LINE_WISE -> "V"
      SelectionType.BLOCK_WISE -> "\u0016"
    }

    Mode.INSERT -> "i"
    is Mode.SELECT -> when (selectionType) {
      SelectionType.CHARACTER_WISE -> "s"
      SelectionType.LINE_WISE -> "S"
      SelectionType.BLOCK_WISE -> "\u0013"
    }

    Mode.REPLACE -> "R"
    is Mode.CMD_LINE -> "c"
    is Mode.OP_PENDING -> "no"
  }
}

fun Mode.returnTo(): Mode {
  return when (this) {
    is Mode.CMD_LINE -> {
      val returnMode = returnTo as Mode
      // We need to understand logic that doesn't exit visual if it's just visual,
      //   but exits visual if it's one-time visual
      if (returnMode.returnTo != null) {
        returnMode.returnTo()
      } else {
        returnMode
      }
    }

    Mode.INSERT -> Mode.NORMAL()
    is Mode.NORMAL -> when (returnTo) {
      ReturnTo.INSERT -> Mode.INSERT
      ReturnTo.REPLACE -> Mode.REPLACE
      null -> Mode.NORMAL()
    }

    is Mode.OP_PENDING -> when (returnTo) {
      ReturnTo.INSERT -> Mode.INSERT
      ReturnTo.REPLACE -> Mode.REPLACE
      null -> Mode.NORMAL()
    }

    Mode.REPLACE -> Mode.NORMAL()
    is Mode.SELECT -> when (returnTo) {
      ReturnTo.INSERT -> Mode.INSERT
      ReturnTo.REPLACE -> Mode.REPLACE
      null -> Mode.NORMAL()
    }

    is Mode.VISUAL -> when (returnTo) {
      ReturnTo.INSERT -> Mode.INSERT
      ReturnTo.REPLACE -> Mode.REPLACE
      null -> Mode.NORMAL()
    }
  }
}

val Mode.toReturnTo: ReturnTo
  get() = when (this) {
    Mode.INSERT -> ReturnTo.INSERT
    Mode.REPLACE -> ReturnTo.REPLACE
    else -> error("Cannot get return to from $this")
  }

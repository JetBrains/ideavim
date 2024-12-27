/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.util.*

inline fun <reified T : Enum<T>> noneOfEnum(): EnumSet<T> = EnumSet.noneOf(T::class.java)

val TextRange.endOffsetInclusive: Int
  get() = if (this.endOffset > 0 && this.endOffset > this.startOffset) this.endOffset - 1 else this.endOffset

val VimEditor.inRepeatMode: Boolean
  get() = injector.vimState.isDotRepeatInProgress

val VimEditor.usesVirtualSpace: Boolean
  get() = injector.options(this).virtualedit.contains(OptionConstants.virtualedit_onemore)

val VimEditor.isEndAllowed: Boolean
  get() = this.isEndAllowed(this.mode)

/**
 * Returns true if the end of line character is allowed as part of motion or selection
 *
 * This is mostly needed for the `$` motion, which can behave differently in different modes, and isn't explicitly
 * documented. The motion is really only valid in Normal and Visual modes. In Normal, it moves to the last character of
 * the current line, not including the end of line character. In Visual (as documented) it moves to the end of line
 * char.
 *
 * The motion obviously doesn't work in Insert or Replace modes, but requires `<C-O>` to enter "Insert Normal" mode.
 * In this case, `$` should move to the end of line char, just like in insert/replace mode. AIUI, this is because Vim
 * will switch to Normal mode with `<C-O>`, set the current column to the "end of line" magic value, return to insert or
 * replace, and then finally update the screen. Because the update happens in Insert/Replace, the "Insert Normal"
 * position for "end of line" becomes the end of line char.
 */
fun VimEditor.isEndAllowed(mode: Mode): Boolean {
  // Technically, we should look at the "ultimate" current mode and skip anything like Command-line or Operator-pending,
  // but for our usages, this isn't necessary
  return when (mode) {
    Mode.INSERT, Mode.REPLACE, is Mode.VISUAL, is Mode.SELECT -> true
    is Mode.NORMAL -> if (mode.isInsertPending || mode.isReplacePending) true else usesVirtualSpace
    is Mode.CMD_LINE, is Mode.OP_PENDING -> usesVirtualSpace
  }
}

inline fun <reified T : Enum<T>> enumSetOf(vararg value: T): EnumSet<T> = when (value.size) {
  0 -> noneOfEnum()
  1 -> EnumSet.of(value[0])
  else -> EnumSet.of(value[0], *value.slice(1..value.lastIndex).toTypedArray())
}

fun VimEditor.setSelectMode(submode: SelectionType) {
  mode = Mode.SELECT(submode, mode.returnTo)
}

fun VimEditor.pushVisualMode(submode: SelectionType) {
  mode = Mode.VISUAL(submode, mode.returnTo)
}

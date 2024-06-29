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
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.isSingleModeActive
import com.maddyhome.idea.vim.state.mode.returnTo
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

fun VimEditor.isEndAllowed(mode: Mode): Boolean {
  return when (mode) {
    is Mode.INSERT, is Mode.VISUAL, is Mode.SELECT -> true
    is Mode.NORMAL, is Mode.CMD_LINE, Mode.REPLACE, is Mode.OP_PENDING -> {
      // One day we'll use a proper insert_normal mode
      if (mode.isSingleModeActive) true else usesVirtualSpace
    }
  }
}

inline fun <reified T : Enum<T>> enumSetOf(vararg value: T): EnumSet<T> = when (value.size) {
  0 -> noneOfEnum()
  1 -> EnumSet.of(value[0])
  else -> EnumSet.of(value[0], *value.slice(1..value.lastIndex).toTypedArray())
}

fun VimEditor.setSelectMode(submode: SelectionType) {
  mode = Mode.SELECT(submode, this.mode.returnTo)
}

fun VimEditor.pushVisualMode(submode: SelectionType) {
  mode = Mode.VISUAL(submode, this.mode.returnTo)
}

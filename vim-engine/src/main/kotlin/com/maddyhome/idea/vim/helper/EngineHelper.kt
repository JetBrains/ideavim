/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.options.OptionConstants
import java.util.*

public inline fun <reified T : Enum<T>> noneOfEnum(): EnumSet<T> = EnumSet.noneOf(T::class.java)

public val TextRange.endOffsetInclusive: Int
  get() = if (this.endOffset > 0 && this.endOffset > this.startOffset) this.endOffset - 1 else this.endOffset

public val VimEditor.mode: VimStateMachine.Mode
  get() = this.vimStateMachine.mode

public val VimEditor.inVisualMode: Boolean
  get() = this.mode.inVisualMode

public val VimEditor.inRepeatMode: Boolean
  get() = this.vimStateMachine.isDotRepeatInProgress

public var VimEditor.subMode: VimStateMachine.SubMode
  get() = this.vimStateMachine.subMode
  set(value) {
    this.vimStateMachine.subMode = value
  }

public val VimEditor.vimStateMachine: VimStateMachine
  get() = VimStateMachine.getInstance(this)

public val VimStateMachine.Mode.inVisualMode: Boolean
  get() = this == VimStateMachine.Mode.VISUAL || this == VimStateMachine.Mode.INSERT_VISUAL

public val VimEditor.inBlockSubMode: Boolean
  get() = this.subMode == VimStateMachine.SubMode.VISUAL_BLOCK

/**
 * Please use `isEndAllowed` based on `Editor` (another extension function)
 * It takes "single command" into account.
 */
public val VimStateMachine.Mode.isEndAllowed: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT -> true
    VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING -> usesVirtualSpace
    VimStateMachine.Mode.INSERT_NORMAL -> usesVirtualSpace
    VimStateMachine.Mode.INSERT_VISUAL -> usesVirtualSpace
    VimStateMachine.Mode.INSERT_SELECT -> usesVirtualSpace
  }

public val usesVirtualSpace: Boolean
  get() = injector.globalOptions().hasValue(Options.virtualedit, OptionConstants.virtualedit_onemore)

public val VimEditor.isEndAllowed: Boolean
  get() = when (this.mode) {
    VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT, VimStateMachine.Mode.INSERT_VISUAL, VimStateMachine.Mode.INSERT_SELECT -> true
    VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING, VimStateMachine.Mode.INSERT_NORMAL -> {
      // One day we'll use a proper insert_normal mode
      if (this.mode.inSingleMode) true else usesVirtualSpace
    }
  }

public val VimStateMachine.Mode.inSingleMode: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT_NORMAL, VimStateMachine.Mode.INSERT_SELECT, VimStateMachine.Mode.INSERT_VISUAL -> true
    else -> false
  }

public val VimStateMachine.Mode.inInsertMode: Boolean
  get() = this == VimStateMachine.Mode.INSERT || this == VimStateMachine.Mode.REPLACE

public val VimStateMachine.Mode.inSingleNormalMode: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT_NORMAL -> true
    else -> false
  }

public val VimEditor.inNormalMode: Boolean
  get() = this.mode.inNormalMode

public val VimStateMachine.Mode.inNormalMode: Boolean
  get() = this == VimStateMachine.Mode.COMMAND || this == VimStateMachine.Mode.INSERT_NORMAL

public val VimStateMachine.Mode.isEndAllowedIgnoringOnemore: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT -> true
    VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING -> false
    VimStateMachine.Mode.INSERT_NORMAL -> false
    VimStateMachine.Mode.INSERT_VISUAL -> true
    VimStateMachine.Mode.INSERT_SELECT -> true
  }

public val VimEditor.inInsertMode: Boolean
  get() = this.mode.inInsertMode

public val VimEditor.inSelectMode: Boolean
  get() = this.mode == VimStateMachine.Mode.SELECT || this.mode == VimStateMachine.Mode.INSERT_SELECT

public val VimEditor.inSingleCommandMode: Boolean
  get() = this.mode.inSingleMode

public inline fun <reified T : Enum<T>> enumSetOf(vararg value: T): EnumSet<T> = when (value.size) {
  0 -> noneOfEnum()
  1 -> EnumSet.of(value[0])
  else -> EnumSet.of(value[0], *value.slice(1..value.lastIndex).toTypedArray())
}

public fun VimStateMachine.pushSelectMode(subMode: VimStateMachine.SubMode, prevMode: VimStateMachine.Mode = this.mode) {
  if (prevMode.inSingleMode) {
    popModes()
    pushModes(VimStateMachine.Mode.INSERT_SELECT, subMode)
  } else {
    pushModes(VimStateMachine.Mode.SELECT, subMode)
  }
}

public fun VimStateMachine.pushVisualMode(subMode: VimStateMachine.SubMode, prevMode: VimStateMachine.Mode = this.mode) {
  if (prevMode.inSingleMode) {
    popModes()
    pushModes(VimStateMachine.Mode.INSERT_VISUAL, subMode)
  } else {
    pushModes(VimStateMachine.Mode.VISUAL, subMode)
  }
}

public fun <K, V> Map<K, V>.firstOrNull(): Map.Entry<K, V>? {
  return this.entries.firstOrNull()
}

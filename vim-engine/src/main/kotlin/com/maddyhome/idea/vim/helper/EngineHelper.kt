package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*

inline fun <reified T : Enum<T>> noneOfEnum(): EnumSet<T> = EnumSet.noneOf(T::class.java)

val TextRange.endOffsetInclusive
  get() = if (this.endOffset > 0 && this.endOffset > this.startOffset) this.endOffset - 1 else this.endOffset

val VimEditor.mode
  get() = this.vimStateMachine.mode

val VimEditor.inVisualMode
  get() = this.mode.inVisualMode

val VimEditor.inRepeatMode
  get() = this.vimStateMachine.isDotRepeatInProgress

var VimEditor.subMode
  get() = this.vimStateMachine.subMode
  set(value) {
    this.vimStateMachine.subMode = value
  }

val VimEditor.vimStateMachine
  get() = VimStateMachine.getInstance(this)

val VimStateMachine.Mode.inVisualMode
  get() = this == VimStateMachine.Mode.VISUAL || this == VimStateMachine.Mode.INSERT_VISUAL

val VimEditor.inBlockSubMode
  get() = this.subMode == VimStateMachine.SubMode.VISUAL_BLOCK

/**
 * Please use `isEndAllowed` based on `Editor` (another extension function)
 * It takes "single command" into account.
 */
val VimStateMachine.Mode.isEndAllowed: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT -> true
    VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING -> usesVirtualSpace
    VimStateMachine.Mode.INSERT_NORMAL -> usesVirtualSpace
    VimStateMachine.Mode.INSERT_VISUAL -> usesVirtualSpace
    VimStateMachine.Mode.INSERT_SELECT -> usesVirtualSpace
  }

val usesVirtualSpace
  get() = (
    injector.optionService.getOptionValue(
      OptionScope.GLOBAL,
      OptionConstants.virtualeditName
    ) as VimString
    ).value == "onemore"

val VimEditor.isEndAllowed: Boolean
  get() = when (this.mode) {
    VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT, VimStateMachine.Mode.INSERT_VISUAL, VimStateMachine.Mode.INSERT_SELECT -> true
    VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING, VimStateMachine.Mode.INSERT_NORMAL -> {
      // One day we'll use a proper insert_normal mode
      if (this.mode.inSingleMode) true else usesVirtualSpace
    }
  }

val VimStateMachine.Mode.inSingleMode: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT_NORMAL, VimStateMachine.Mode.INSERT_SELECT, VimStateMachine.Mode.INSERT_VISUAL -> true
    else -> false
  }

val VimStateMachine.Mode.inInsertMode: Boolean
  get() = this == VimStateMachine.Mode.INSERT || this == VimStateMachine.Mode.REPLACE

val VimStateMachine.Mode.inSingleNormalMode: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT_NORMAL -> true
    else -> false
  }

val VimEditor.inNormalMode
  get() = this.mode.inNormalMode

val VimStateMachine.Mode.inNormalMode
  get() = this == VimStateMachine.Mode.COMMAND || this == VimStateMachine.Mode.INSERT_NORMAL

val VimStateMachine.Mode.isEndAllowedIgnoringOnemore: Boolean
  get() = when (this) {
    VimStateMachine.Mode.INSERT, VimStateMachine.Mode.VISUAL, VimStateMachine.Mode.SELECT -> true
    VimStateMachine.Mode.COMMAND, VimStateMachine.Mode.CMD_LINE, VimStateMachine.Mode.REPLACE, VimStateMachine.Mode.OP_PENDING -> false
    VimStateMachine.Mode.INSERT_NORMAL -> false
    VimStateMachine.Mode.INSERT_VISUAL -> true
    VimStateMachine.Mode.INSERT_SELECT -> true
  }

val VimEditor.inInsertMode
  get() = this.mode.inInsertMode

val VimEditor.inSelectMode
  get() = this.mode == VimStateMachine.Mode.SELECT || this.mode == VimStateMachine.Mode.INSERT_SELECT

val VimEditor.inSingleCommandMode
  get() = this.mode.inSingleMode

inline fun <reified T : Enum<T>> enumSetOf(vararg value: T): EnumSet<T> = when (value.size) {
  0 -> noneOfEnum()
  1 -> EnumSet.of(value[0])
  else -> EnumSet.of(value[0], *value.slice(1..value.lastIndex).toTypedArray())
}

fun VimStateMachine.pushSelectMode(subMode: VimStateMachine.SubMode, prevMode: VimStateMachine.Mode = this.mode) {
  if (prevMode.inSingleMode) {
    popModes()
    pushModes(VimStateMachine.Mode.INSERT_SELECT, subMode)
  } else {
    pushModes(VimStateMachine.Mode.SELECT, subMode)
  }
}

fun VimStateMachine.pushVisualMode(subMode: VimStateMachine.SubMode, prevMode: VimStateMachine.Mode = this.mode) {
  if (prevMode.inSingleMode) {
    popModes()
    pushModes(VimStateMachine.Mode.INSERT_VISUAL, subMode)
  } else {
    pushModes(VimStateMachine.Mode.VISUAL, subMode)
  }
}

fun <K, V> Map<K, V>.firstOrNull(): Map.Entry<K, V>? {
  return this.entries.firstOrNull()
}

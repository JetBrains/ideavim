/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.Mode
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.forgetAllReplaceMasks
import com.maddyhome.idea.vim.impl.state.VimStateMachineImpl
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import com.maddyhome.idea.vim.state.mode.inCommandLineModeWithVisual
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.Mode as EngineMode

fun EngineMode.toMode(): Mode {
  return when (this) {
    is EngineMode.NORMAL -> Mode.NORMAL(if (returnTo == this) Mode.NORMAL() else returnTo.toMode())
    is EngineMode.VISUAL -> Mode.VISUAL(
      selectionType.toTextSelectionType(),
      if (returnTo == this) Mode.NORMAL() else returnTo.toMode()
    )

    is EngineMode.SELECT -> Mode.SELECT(
      selectionType.toTextSelectionType(),
      if (returnTo == this) Mode.NORMAL() else returnTo.toMode()
    )

    is EngineMode.OP_PENDING -> Mode.OP_PENDING(if (returnTo == this) Mode.NORMAL() else returnTo.toMode())
    is EngineMode.INSERT -> Mode.INSERT
    is EngineMode.CMD_LINE -> Mode.CMD_LINE(if (returnTo == this) Mode.NORMAL() else returnTo.toMode())
    is EngineMode.REPLACE -> Mode.REPLACE
  }
}

fun Mode.toEngineMode(): EngineMode {
  val returnTo = this.returnTo
  return when (this) {
    is Mode.NORMAL -> EngineMode.NORMAL()
    is Mode.VISUAL -> {
      EngineMode.VISUAL(
        selectionType.toSelectionType(),
        returnTo.toEngineMode()
      )
    }

    is Mode.SELECT -> EngineMode.SELECT(
      selectionType.toSelectionType(),
      returnTo.toEngineMode()
    )

    is Mode.OP_PENDING -> EngineMode.OP_PENDING(returnTo.toEngineMode())
    is Mode.INSERT -> EngineMode.INSERT
    is Mode.CMD_LINE -> EngineMode.CMD_LINE(returnTo.toEngineMode())
    is Mode.REPLACE -> EngineMode.REPLACE
  }
}

fun changeMode(value: Mode, vimEditor: VimEditor?) {
  val vimState = injector.vimState
  val currentMode: EngineMode = vimState.mode
  if (currentMode == value) return

  val oldValue: EngineMode = vimState.mode
  if (oldValue == EngineMode.REPLACE) {
    forgetAllReplaceMasks()
  } else if (oldValue is EngineMode.VISUAL && vimEditor != null) {
    val selectionType = oldValue.selectionType

    // remove carets and selection
    SelectionVimListenerSuppressor.lock().use {
      if (vimEditor.inBlockSelection) {
        vimEditor.removeSecondaryCarets()
      }
      injector.application.runWriteAction {
        vimEditor.nativeCarets().forEach(VimCaret::removeSelection)
      }
    }

    // set selection marks
    if (vimEditor.inVisualMode || vimEditor.inCommandLineModeWithVisual) {
      vimEditor.vimLastSelectionType = selectionType
      injector.application.runReadAction {
        injector.markService.setVisualSelectionMarks(vimEditor)
      }
      vimEditor.nativeCarets().forEach { it.vimSelectionStartClear() }
    }
  }

  (injector.vimState as VimStateMachineImpl).mode = value.toEngineMode()
  val editor = vimEditor ?: injector.fallbackWindow
  // todo: we should probably remove editor from ModeChangeListener, but listener implementations use it
  injector.listenersNotifier.notifyModeChanged(editor, oldValue)
}
/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.group.visual.VimVisualTimer.mode
import com.maddyhome.idea.vim.group.visual.VimVisualTimer.singleTask
import java.awt.event.ActionEvent
import javax.swing.Timer

/**
 * Timer to control non-vim visual selection adjustment.
 *
 * Some actions in IJ use selection as a helper to reach desired behavior. E.g. backspace in python or yaml uses
 *   selection to remove indent on the empty line. It selects indent and after that performs "remove selection" action.
 *   If IdeaVim had reacted to this selection immediately, its listeners were called twice: after selection set and
 *   after selection removing. After there both operations vim would get normal mode, what is inconvenient because user
 *   expects vim to stay in insert mode.
 *   Same approach is used in some accented characters on macOS and for Romaji layout in Japanese language (VIM-1725).
 *
 * Because of this IdeaVim doesn't react to selection change immediately. After selection was changed, vim waits some
 *   time (100 ms by default) and enables visual mode (or any another) if selection was not changed during this time.
 *
 * If during timer sleep another selection change was performed, the old timer get's disabled and
 *   replaced with a new one.
 *
 * So, how does IdeaVim recognize that it should stay in insert mode?
 * Timer also gets an information about the current mode and whenever editor has selection.
 *   [mode] property stores information about mode of *the first timer call*. So, if there were several calls
 *   to timer, the first mode state will be stored in [mode] property. When the last timer acts, it compares state
 *   of selection before first call and expected selection state after current call. Selection adjustment gets
 *   performed only if there are changes in selection.
 *
 * Examples:
 * 1) User performs "extend selection" action from normal mode
 *
 * There will be only one call to [singleTask]. [mode] is command, editorHasSelection is true. So, selection adjustment
 *   is called, IdeaVim starts visual (or select) mode.
 *
 * 2) Some action performs "extend selection" and "shrink selection" actions sequentially from insert mode.
 *
 * First call to [singleTask]. [mode] is insert, editorHasSelection is true. Timer starts.
 * Second call to [singleTask]. First timer gets interrupted. [mode] stays insert because it's not null.
 *   editorHasSelection is false. Insert mode ([mode]) has no selection and editor also has no selection, so
 *   no adjustment gets performed and IdeaVim stays in insert mode.
 */
internal object VimVisualTimer {

  var swingTimer: Timer? = null
  var mode: VimStateMachine.Mode? = null

  inline fun singleTask(currentMode: VimStateMachine.Mode, crossinline task: (initialMode: VimStateMachine.Mode?) -> Unit) {
    swingTimer?.stop()

    if (mode == null) mode = currentMode

    // Default delay - 100 ms
    val timer = Timer(injector.globalOptions().getIntValue(IjOptionConstants.visualdelay)) {
      timerAction(task)
    }
    timer.isRepeats = false
    timer.start()
    swingTimer = timer
  }

  fun doNow() {
    val swingTimer1 = swingTimer
    if (swingTimer1 != null) {
      swingTimer1.stop()
      swingTimer1.actionListeners?.forEach {
        it.actionPerformed(ActionEvent(swingTimer1, 0, swingTimer1.actionCommand, System.currentTimeMillis(), 0))
      }
    }
  }

  inline fun timerAction(task: (initialMode: VimStateMachine.Mode?) -> Unit) {
    task(mode)
    swingTimer = null
    mode = null
  }
}

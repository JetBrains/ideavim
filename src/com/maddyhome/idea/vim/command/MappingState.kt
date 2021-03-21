/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.annotations.TestOnly
import java.awt.event.ActionListener
import javax.swing.KeyStroke
import javax.swing.Timer

class MappingState {
  val keys: Iterable<KeyStroke>
    get() = keyList

  var mappingMode = MappingMode.NORMAL

  private val timer = Timer(OptionsManager.timeoutlen.value(), null)
  private var keyList = mutableListOf<KeyStroke>()

  init {
    timer.isRepeats = false
  }

  fun startMappingTimer(actionListener: ActionListener) {
    timer.initialDelay = OptionsManager.timeoutlen.value()
    timer.actionListeners.forEach { timer.removeActionListener(it) }
    timer.addActionListener(actionListener)
    timer.start()
  }

  fun stopMappingTimer() {
    timer.stop()
    timer.actionListeners.forEach { timer.removeActionListener(it) }
  }

  @TestOnly
  fun isTimerRunning(): Boolean = timer.isRunning

  fun addKey(key: KeyStroke) {
    keyList.add(key)
  }

  fun detachKeys(): List<KeyStroke> {
    val currentKeys = keyList
    keyList = mutableListOf()
    return currentKeys
  }

  fun resetMappingSequence() {
    stopMappingTimer()
    keyList.clear()
    // NOTE: We intentionally don't reset mapping mode here
  }
}

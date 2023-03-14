/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.event.ActionListener
import javax.swing.KeyStroke
import javax.swing.Timer

public class MappingState {
  // Map command depth. 0 - if it is not a map command. 1 - regular map command. 2+ - nested map commands
  private var mapDepth = 0

  public fun isExecutingMap(): Boolean {
    return mapDepth > 0
  }

  public fun startMapExecution() {
    ++mapDepth
  }

  public fun stopMapExecution() {
    --mapDepth
  }

  public val keys: Iterable<KeyStroke>
    get() = keyList

  public var mappingMode: MappingMode = MappingMode.NORMAL

  private val timer = Timer(injector.globalOptions().getIntValue(Options.timeoutlen), null)
  private var keyList = mutableListOf<KeyStroke>()

  init {
    timer.isRepeats = false
  }

  public fun startMappingTimer(actionListener: ActionListener) {
    timer.initialDelay = injector.globalOptions().getIntValue(Options.timeoutlen)
    timer.actionListeners.forEach { timer.removeActionListener(it) }
    timer.addActionListener(actionListener)
    timer.start()
  }

  public fun stopMappingTimer() {
    LOG.trace { "Stop mapping timer" }
    timer.stop()
    timer.actionListeners.forEach { timer.removeActionListener(it) }
  }

  public fun addKey(key: KeyStroke) {
    keyList.add(key)
  }

  public fun detachKeys(): List<KeyStroke> {
    val currentKeys = keyList
    keyList = mutableListOf()
    return currentKeys
  }

  public fun resetMappingSequence() {
    LOG.trace("Reset mapping sequence")
    stopMappingTimer()
    keyList.clear()
    // NOTE: We intentionally don't reset mapping mode here
  }

  public companion object {
    private val LOG = vimLogger<MappingState>()
  }
}

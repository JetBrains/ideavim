/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.event.ActionListener
import javax.swing.KeyStroke
import javax.swing.Timer

class MappingState: Cloneable {
  // Map command depth. 0 - if it is not a map command. 1 - regular map command. 2+ - nested map commands
  private var mapDepth = 0

  @Deprecated("This function is created only for binary compatibility")
  fun getMappingMode(): MappingMode = MappingMode.NORMAL

  fun isExecutingMap(): Boolean {
    return mapDepth > 0
  }

  fun startMapExecution() {
    ++mapDepth
  }

  fun stopMapExecution() {
    --mapDepth
  }

  val keys: Iterable<KeyStroke>
    get() = keyList
  
  val hasKeys
    get() = keyList.isNotEmpty()

  private var timer = VimTimer(injector.globalOptions().timeoutlen)
  private var keyList = mutableListOf<KeyStroke>()

  init {
    timer.isRepeats = false
  }

  fun startMappingTimer(actionListener: ActionListener) {
    timer.initialDelay = injector.globalOptions().timeoutlen
    timer.actionListeners.forEach { timer.removeActionListener(it) }
    timer.addActionListener(actionListener)
    timer.start()
  }

  fun stopMappingTimer() {
    LOG.trace { "Stop mapping timer" }
    timer.stop()
    timer.actionListeners.forEach { timer.removeActionListener(it) }
  }

  fun addKey(key: KeyStroke) {
    keyList.add(key)
  }

  fun detachKeys(): List<KeyStroke> {
    val currentKeys = keyList
    keyList = mutableListOf()
    return currentKeys
  }

  fun resetMappingSequence() {
    LOG.trace("Reset mapping sequence")
    stopMappingTimer()
    keyList.clear()
    // NOTE: We intentionally don't reset mapping mode here
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MappingState

    if (mapDepth != other.mapDepth) return false
    if (timer != other.timer) return false
    if (keyList != other.keyList) return false

    return true
  }

  override fun hashCode(): Int {
    var result = mapDepth
    result = 31 * result + timer.hashCode()
    result = 31 * result + keyList.hashCode()
    return result
  }

  public override fun clone(): MappingState {
    val result = MappingState()
    result.timer = timer
    result.mapDepth = mapDepth
    result.keyList = keyList.toMutableList()
    return result
  }

  override fun toString(): String {
    return "Map depth = $mapDepth, keys = ${ injector.parser.toKeyNotation(keys.toList()) }"
  }

  companion object {
    private val LOG = vimLogger<MappingState>()
  }

  class VimTimer(delay: Int) : Timer(delay, null) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as VimTimer

      if (delay != other.delay) return false
      if (initialDelay != other.initialDelay) return false
      if (isRunning != other.isRunning) return false

      return true
    }

    override fun hashCode(): Int {
      return javaClass.hashCode()
    }
  }
}
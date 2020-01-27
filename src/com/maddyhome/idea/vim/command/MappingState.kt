package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.option.OptionsManager
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

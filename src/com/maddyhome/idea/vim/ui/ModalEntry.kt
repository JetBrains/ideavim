package com.maddyhome.idea.vim.ui

import com.maddyhome.idea.vim.helper.StringHelper
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
object ModalEntry {
  inline fun activate(crossinline processor: (KeyStroke) -> Boolean) {
    val systemQueue = Toolkit.getDefaultToolkit().systemEventQueue
    val loop = systemQueue.createSecondaryLoop()

    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(object : KeyEventDispatcher {
      override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val stroke: KeyStroke
        if (e.id == KeyEvent.KEY_RELEASED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e)
          if (!StringHelper.isCloseKeyStroke(stroke) && stroke.keyCode != KeyEvent.VK_ENTER) {
            return true
          }
        } else if (e.id == KeyEvent.KEY_TYPED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e)
        } else {
          return true
        }
        if (!processor(stroke)) {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this)
          loop.exit()
        }
        return true
      }
    })

    loop.enter()
  }
}

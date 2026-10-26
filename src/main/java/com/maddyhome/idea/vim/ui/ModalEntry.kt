/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author dhleong
 */
object ModalEntry {

  val LOG: Logger = logger<ModalEntry>()

  inline fun activate(editor: VimEditor, crossinline processor: (KeyStroke) -> Boolean) {
    // Firstly we pull the unfinished keys of the current mapping
    val mappingStack = KeyHandler.getInstance().keyStack
    LOG.trace("Dumping key stack:")
    LOG.trace { mappingStack.dump() }
    var stroke = mappingStack.feedSomeStroke()
    while (stroke != null) {
      val result = processor(stroke)
      if (!result) {
        LOG.trace("Got char from mapping stack")
        return
      }
      stroke = mappingStack.feedSomeStroke()
    }

    // Then start to accept user input
    val systemQueue = Toolkit.getDefaultToolkit().systemEventQueue
    val loop = systemQueue.createSecondaryLoop()

    // The dispatcher gets the keys before the keymap. Otherwise a key that is also a shortcut, e.g. <CR> or <Esc>, would
    // first run the IdeaVim or IDE action bound to it, and only then reach the processor (VIM-2796)
    val dispatcherDisposable = Disposer.newDisposable("ModalEntry")
    IdeEventQueue.getInstance().addDispatcher(object : IdeEventQueue.NonLockedEventDispatcher {
      override fun dispatch(e: AWTEvent): Boolean {
        if (e !is KeyEvent) return false
        val stroke: KeyStroke
        if (e.id == KeyEvent.KEY_RELEASED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e)
          if (!stroke.isCloseKeyStroke() && stroke.keyCode != KeyEvent.VK_ENTER) {
            return true
          }
        } else if (e.id == KeyEvent.KEY_TYPED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e)
          // <Esc> and <CR> are taken when the key is released. Their typed character would be read as text
          if (stroke.isCloseKeyStroke() || stroke.keyChar == '\n') {
            return true
          }
        } else {
          return true
        }
        if (injector.registerGroup.isRecording) {
          KeyHandler.getInstance().modalEntryKeys += stroke
        }
        if (!processor(stroke)) {
          LOG.trace("Got char from keyboard input: $stroke. Event: $e")
          Disposer.dispose(dispatcherDisposable)
          loop.exit()
        }
        return true
      }
    }, dispatcherDisposable)

    loop.enter()
  }
}

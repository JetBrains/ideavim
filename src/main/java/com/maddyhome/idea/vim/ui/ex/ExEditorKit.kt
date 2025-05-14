/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.annotations.NonNls
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.text.DefaultEditorKit
import javax.swing.text.Document

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal object ExEditorKit : DefaultEditorKit() {
  /**
   * Gets the MIME type of the data that this
   * kit represents support for.
   *
   * @return the type
   */
  @NonNls
  override fun getContentType(): String {
    return "text/ideavim"
  }

  /**
   * Creates an uninitialized text storage model
   * that is appropriate for this type of editor.
   *
   * @return the model
   */
  override fun createDefaultDocument(): Document {
    return ExDocument()
  }

  class DefaultExKeyHandler : DefaultKeyTypedAction() {
    override fun actionPerformed(e: ActionEvent) {
      val target = getTextComponent(e) as ExTextField

      val key = convert(e)
      if (key != null) {
        val c = key.keyChar
        if (c.code > 0) {
          if (target.useHandleKeyFromEx) {
            val panel = ((injector.commandLine.getActiveCommandLine() as? ExEntryPanel)
              ?: (injector.modalInput.getCurrentModalInput() as? WrappedAsModalInputExEntryPanel)?.exEntryPanel)
              ?: return
            val editor = panel.ijEditor
            val keyHandler = KeyHandler.getInstance()
            keyHandler.handleKey(editor!!.vim, key, panel.context.vim, keyHandler.keyHandlerState)
          } else {
            val event = ActionEvent(e.source, e.id, c.toString(), e.getWhen(), e.modifiers)
            super.actionPerformed(event)
          }
          target.saveLastEntry()
        }
      } else {
        super.actionPerformed(e)
        target.saveLastEntry()
      }
    }
  }

  fun convert(event: ActionEvent): KeyStroke? {
    val cmd = event.actionCommand
    val mods = event.modifiers
    if (cmd != null && cmd.isNotEmpty()) {
      val ch = cmd[0]
      if (ch < ' ') {
        if (mods and ActionEvent.CTRL_MASK != 0) {
          return KeyStroke.getKeyStroke(KeyEvent.VK_A + ch.code - 1, mods)
        }
      } else {
        return KeyStroke.getKeyStroke(Character.valueOf(ch), mods)
      }
    }
    return null
  }
}

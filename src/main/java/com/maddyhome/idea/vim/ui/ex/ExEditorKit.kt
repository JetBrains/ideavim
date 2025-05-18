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

     val key = convertActionEventToKeyStroke(e)
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

  fun convertActionEventToKeyStroke(event: ActionEvent): KeyStroke? {
    val cmd = event.actionCommand
    val mods = event.modifiers
    if (cmd != null && cmd.isNotEmpty()) {
      // event.actionCommand is null if KeyStroke.keyChar is KeyEvent.UNDEFINED. Which means cmd is either an actual
      // action command (it's not, we don't have any registered) or it's the string version of the key char. In which
      // case, it's either a typed, printable character, or it's a control character. If it's a control character, it's
      // the actual character, e.g. `\n`, '\t` or ESC (ASCII 27) without the CTRL modifier.
      if (cmd[0] < ' ') {
        // If it's a control character, convert it into a KEY_PRESSED KeyStroke, i.e. based on keyCode rather than a
        // converted key char. Get the virtual key code for the control character. Note that this might be incorrect,
        // as there aren't virtual key codes for most control characters, only VK_BACK_SPACE, VK_TAB, VK_ENTER and
        // VK_ESCAPE.
        // TODO: This will cause trouble when we want to enter a control character as a literal
        // We'll cross that bridge when we come to it...
        // I think the correct implementation is to handle the actual KeyEvent, rather than let Swing convert the
        // KeyStroke to an ActionEvent and then try to convert it back again
        val keyCode = KeyEvent.getExtendedKeyCodeForChar(cmd[0].code)
        return KeyStroke.getKeyStroke(keyCode, mods)
      }
      else {
        // The command is a typed character, so treat it as a KEY_TYPED KeyStroke, based on a converted character
        return KeyStroke.getKeyStroke(cmd[0], mods)
      }
    }
    return null
  }
}

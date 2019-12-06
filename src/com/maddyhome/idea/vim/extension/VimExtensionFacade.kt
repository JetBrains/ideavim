/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
package com.maddyhome.idea.vim.extension

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.change.VimRepeater
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.ui.ExEntryPanel
import com.maddyhome.idea.vim.ui.ModalEntry
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Vim API facade that defines functions similar to the built-in functions and statements of the original Vim.
 *
 * See :help eval.
 *
 * @author vlan
 */
object VimExtensionFacade {
  /** The 'map' command for mapping keys to handlers defined in extensions. */
  @JvmStatic
  fun putExtensionHandlerMapping(modes: Set<MappingMode>, fromKeys: List<KeyStroke>,
                                 extensionHandler: VimExtensionHandler, recursive: Boolean) {
    VimPlugin.getKey().putKeyMapping(modes, fromKeys, null, extensionHandler, recursive)
  }

  /** The 'map' command for mapping keys to other keys. */
  @JvmStatic
  fun putKeyMapping(modes: Set<MappingMode>, fromKeys: List<KeyStroke>,
                    toKeys: List<KeyStroke>, recursive: Boolean) {
    VimPlugin.getKey().putKeyMapping(modes, fromKeys, toKeys, null, recursive)
  }

  /** Sets the value of 'operatorfunc' to be used as the operator function in 'g@'. */
  @JvmStatic
  fun setOperatorFunction(function: OperatorFunction) {
    VimPlugin.getKey().setOperatorFunction(function)
  }

  /**
   * Runs normal mode commands similar to ':normal {commands}'.
   *
   * XXX: Currently it doesn't make the editor enter the normal mode, it doesn't recover from incomplete commands, it
   * leaves the editor in the insert mode if it's been activated.
   */
  @JvmStatic
  fun executeNormal(keys: List<KeyStroke>, editor: Editor) {
    val context = EditorDataContext(editor)
    keys.forEach { KeyHandler.getInstance().handleKey(editor, it, context) }
  }

  /** Returns a single key stroke from the user input similar to 'getchar()'. */
  @JvmStatic
  fun inputKeyStroke(editor: Editor): KeyStroke {
    if (editor.commandState.isDotRepeatInProgress) {
      val input = VimRepeater.Extension.consumeKeystroke()
      return input ?: throw RuntimeException("Not enough keystrokes saved: ${VimRepeater.Extension.lastExtensionHandler}")
    }

    val key: KeyStroke? = if (ApplicationManager.getApplication().isUnitTestMode) {
      TestInputModel.getInstance(editor).nextKeyStroke()
    } else {
      var ref: KeyStroke? = null
      ModalEntry.activate { stroke: KeyStroke? ->
        ref = stroke
        false
      }
      ref
    }
    val result = key ?: KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE.toChar())
    VimRepeater.Extension.addKeystroke(result)
    return result
  }

  /** Returns a string typed in the input box similar to 'input()'. */
  @JvmStatic
  fun inputString(editor: Editor, prompt: String, finishOn: Char?): String {
    if (editor.commandState.isDotRepeatInProgress) {
      val input = VimRepeater.Extension.consumeString()
      return input ?: throw RuntimeException("Not enough strings saved: ${VimRepeater.Extension.lastExtensionHandler}")
    }

    if (ApplicationManager.getApplication().isUnitTestMode) {
      val builder = StringBuilder()
      val inputModel = TestInputModel.getInstance(editor)
      var key: KeyStroke? = inputModel.nextKeyStroke()
      while (key != null
        && !StringHelper.isCloseKeyStroke(key) && key.keyCode != KeyEvent.VK_ENTER
        && (finishOn == null || key.keyChar != finishOn)) {
        val c = key.keyChar
        if (c != KeyEvent.CHAR_UNDEFINED) {
          builder.append(c)
        }
        key = inputModel.nextKeyStroke()
      }
      if (finishOn != null && key != null && key.keyChar == finishOn) {
        builder.append(key.keyChar)
      }
      VimRepeater.Extension.addString(builder.toString())
      return builder.toString()
    } else {
      var text = ""
      // XXX: The Ex entry panel is used only for UI here, its logic might be inappropriate for input()
      val exEntryPanel = ExEntryPanel.getInstanceWithoutShortcuts()
      exEntryPanel.activate(editor, EditorDataContext(editor), if (prompt.isEmpty()) " " else prompt, "", 1)
      ModalEntry.activate { key: KeyStroke ->
        return@activate when {
          StringHelper.isCloseKeyStroke(key) -> {
            exEntryPanel.deactivate(true)
            false
          }
          key.keyCode == KeyEvent.VK_ENTER -> {
            text = exEntryPanel.text
            exEntryPanel.deactivate(true)
            false
          }
          finishOn != null && key.keyChar == finishOn -> {
            exEntryPanel.handleKey(key)
            text = exEntryPanel.text
            exEntryPanel.deactivate(true)
            false
          }
          else -> {
            exEntryPanel.handleKey(key)
            true
          }
        }
      }
      VimRepeater.Extension.addString(text)
      return text;
    }
  }

  /** Get the current contents of the given register similar to 'getreg()'. */
  @JvmStatic
  fun getRegister(register: Char): List<KeyStroke>? {
    val reg = VimPlugin.getRegister().getRegister(register) ?: return null
    return reg.keys
  }

  /** Set the current contents of the given register */
  @JvmStatic
  fun setRegister(register: Char, keys: List<KeyStroke?>?) {
    VimPlugin.getRegister().setKeys(register, keys ?: emptyList())
  }
}

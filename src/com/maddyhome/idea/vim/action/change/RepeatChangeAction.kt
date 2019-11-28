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
package com.maddyhome.idea.vim.action.change

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.commandState
import javax.swing.KeyStroke

class RepeatChangeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val state = editor.commandState
    val lastCommand = VimRepeater.lastChangeCommand

    if (lastCommand == null && VimRepeater.Extension.lastExtensionHandler == null) return false

    // Save state
    val save = state.command
    val lastFTCmd = VimPlugin.getMotion().lastFTCmd
    val lastFTChar = VimPlugin.getMotion().lastFTChar
    val reg = VimPlugin.getRegister().currentRegister
    val lastHandler = VimRepeater.Extension.lastExtensionHandler
    val repeatHandler = VimRepeater.repeatHandler

    state.isDotRepeatInProgress = true
    VimPlugin.getRegister().selectRegister(VimRepeater.lastChangeRegister)

    try {
      if (repeatHandler && lastHandler != null) {
        val processor = CommandProcessor.getInstance()
        processor.executeCommand(editor.project, { lastHandler.execute(editor, context) }, "Vim " + lastHandler.javaClass.simpleName, null)
      } else if (!repeatHandler && lastCommand != null) {
        if (cmd.rawCount > 0) {
          lastCommand.count = cmd.count
          val arg = lastCommand.argument
          if (arg != null) {
            val mot = arg.motion
            mot.count = 0
          }
        }
        state.setCommand(lastCommand)

        KeyHandler.executeVimAction(editor, lastCommand.action, context)

        VimRepeater.saveLastChange(lastCommand)
      }
    } catch (ignored: Exception) {
    }

    state.isDotRepeatInProgress = false

    // Restore state
    if (save != null) state.setCommand(save)
    VimPlugin.getMotion().setLastFTCmd(lastFTCmd, lastFTChar)
    if (lastHandler != null) VimRepeater.Extension.lastExtensionHandler = lastHandler
    VimRepeater.repeatHandler = repeatHandler
    VimRepeater.Extension.reset()
    VimPlugin.getRegister().selectRegister(reg)
    return true
  }
}

object VimRepeater {
  var repeatHandler = false

  var lastChangeCommand: Command? = null
    private set
  var lastChangeRegister = VimPlugin.getRegister().defaultRegister
    private set

  fun saveLastChange(command: Command) {
    lastChangeCommand = command
    lastChangeRegister = VimPlugin.getRegister().currentRegister
  }

  object Extension {
    var lastExtensionHandler: VimExtensionHandler? = null
    var argumentCaptured: Argument? = null

    private val keyStrokes = mutableListOf<KeyStroke>()
    private val strings = mutableListOf<String>()

    private var keystrokePointer = 0
    private var stringPointer = 0

    fun addKeystroke(key: KeyStroke) = keyStrokes.add(key)
    fun addString(key: String) = strings.add(key)

    fun consumeKeystroke(): KeyStroke? {
      if (keystrokePointer in keyStrokes.indices) {
        keystrokePointer += 1
        return keyStrokes[keystrokePointer - 1]
      }
      return null
    }

    fun consumeString(): String? {
      if (stringPointer in strings.indices) {
        stringPointer += 1
        return strings[stringPointer - 1]
      }
      return null
    }

    fun reset() {
      keystrokePointer = 0
      stringPointer = 0
    }

    fun clean() {
      keyStrokes.clear()
      strings.clear()
      keystrokePointer = 0
      stringPointer = 0
    }
  }
}

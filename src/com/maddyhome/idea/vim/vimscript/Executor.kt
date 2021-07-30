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

package com.maddyhome.idea.vim.vimscript

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import java.io.File
import java.io.IOException

object Executor {
  private val logger = logger<Executor>()
  var executingVimScript = false

  @kotlin.jvm.Throws(ExException::class)
  fun execute(script: String, editor: Editor?, context: DataContext?, skipHistory: Boolean) {
    try {
      val executableUnit = VimscriptParser.parse(script)
      val vimContext = VimContext()
      executableUnit.execute(editor, context, vimContext, skipHistory)
    } catch (e: ExException) {
      VimPlugin.showMessage(e.message)
      VimPlugin.indicateError()
    }
  }

  fun execute(script: String, skipHistory: Boolean = true) {
    execute(script, null, null, skipHistory)
  }

  @JvmStatic
  fun executeFile(file: File) {
    try {
      execute(file.readText(), false)
    } catch (ignored: IOException) {
    }
  }

  @kotlin.jvm.Throws(ExException::class)
  fun executeLastCommand(editor: Editor, context: DataContext): Boolean {
    val reg = VimPlugin.getRegister().getRegister(':') ?: return false
    val text = reg.text ?: return false
    execute(text, editor, context, false)
    return true
  }
}

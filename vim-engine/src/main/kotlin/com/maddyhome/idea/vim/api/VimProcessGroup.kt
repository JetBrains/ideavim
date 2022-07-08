/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.Command
import javax.swing.KeyStroke

interface VimProcessGroup {
  val lastCommand: String?

  fun startSearchCommand(editor: VimEditor, context: ExecutionContext?, count: Int, leader: Char)
  fun endSearchCommand(): String
  fun processExKey(editor: VimEditor, stroke: KeyStroke): Boolean
  fun startFilterCommand(editor: VimEditor, context: ExecutionContext?, cmd: Command)
  fun startExCommand(editor: VimEditor, context: ExecutionContext?, cmd: Command)
  fun processExEntry(editor: VimEditor, context: ExecutionContext): Boolean
  fun cancelExEntry(editor: VimEditor, resetCaret: Boolean)
  @kotlin.jvm.Throws(java.lang.Exception::class)
  fun executeCommand(editor: VimEditor, command: String, input: CharSequence?, currentDirectoryPath: String?): String?
}

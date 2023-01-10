/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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

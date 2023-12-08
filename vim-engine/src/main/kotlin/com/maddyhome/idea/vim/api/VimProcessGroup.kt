/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

public interface VimProcessGroup {
  public val lastCommand: String?
  public val isCommandProcessing: Boolean
  public val modeBeforeCommandProcessing: Mode?

  public fun startSearchCommand(editor: VimEditor, context: ExecutionContext, count: Int, leader: Char)
  public fun endSearchCommand(): String
  public fun processExKey(editor: VimEditor, stroke: KeyStroke): Boolean
  public fun startFilterCommand(editor: VimEditor, context: ExecutionContext, cmd: Command)
  public fun startExCommand(editor: VimEditor, context: ExecutionContext, cmd: Command)
  public fun processExEntry(editor: VimEditor, context: ExecutionContext): Boolean
  public fun cancelExEntry(editor: VimEditor, resetCaret: Boolean)

  @kotlin.jvm.Throws(java.lang.Exception::class)
  public fun executeCommand(editor: VimEditor, command: String, input: CharSequence?, currentDirectoryPath: String?): String?
}

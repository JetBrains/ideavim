/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

interface VimProcessGroup {
  val lastCommand: String?
  var isCommandProcessing: Boolean
  var modeBeforeCommandProcessing: Mode?

  fun startExEntry(editor: VimEditor, context: ExecutionContext, command: Command, label: String = ":", initialCommandText: String = "")
  // TODO remove me
  // TODO: Why ^^ ? Should that also include startExEntry?
  fun processExKey(editor: VimEditor, stroke: KeyStroke, processResultBuilder: KeyProcessResult.KeyProcessResultBuilder): Boolean
  fun cancelExEntry(editor: VimEditor, refocusOwningEditor: Boolean, resetCaret: Boolean)

  @kotlin.jvm.Throws(java.lang.Exception::class)
  fun executeCommand(editor: VimEditor, command: String, input: CharSequence?, currentDirectoryPath: String?): String?
}
